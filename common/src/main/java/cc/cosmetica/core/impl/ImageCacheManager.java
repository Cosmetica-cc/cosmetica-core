package cc.cosmetica.core.impl;

import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ImageCacheManager {
    /**
     * Create and load a new cache manager.
     * @throws IOException if an IO exception occurs setting up.
     */
    public ImageCacheManager(Path file) throws IOException {
        this.file = file;
    }

    private final Path file;
    private List<ResourceLocation> keep = new ArrayList<>(); // max size 256
    private final Map<String, List<CacheMeta>> entries = new HashMap<>();

    public void saveSync() throws IOException {
        Logging.getInstance().debug("Saving cosmetica image cache metadata (sync)");
        this.save(this.keep, this.entries);
    }

    public void saveAsync() throws IOException {
        // gather data on main thread, save off thread
        Logging.getInstance().debug("Saving cosmetica image cache metadata (async)");
        long timestamp = System.nanoTime();

        final Map<String, List<CacheMeta>> saveEntries = new HashMap<>();
        entries.forEach((s,e)->{
            List<CacheMeta> entries1 = new ArrayList<>();
            for (CacheMeta meta : entries1) {
                CacheMeta cloneMeta = new CacheMeta();
                cloneMeta.subfolder = meta.subfolder;
                cloneMeta.timestamps.putAll(meta.timestamps);
                entries1.add(cloneMeta);
            }
            saveEntries.put(s, entries1);
        });

        long dt = (System.nanoTime() - timestamp) / 1_000_000;
        if (dt >= 10) { // 50ms in a tick. should be much less!
            Logging.getInstance().warn("(Cosmetica Core) Image cache metadata clone took {} ms!", dt);
        }

        final List<ResourceLocation> keep = new ArrayList<>(this.keep);
        // launch save async
        CompletableFuture.runAsync(() -> this.save(keep, saveEntries), Util.backgroundExecutor());
    }

    private void save(List<ResourceLocation> keep, Map<String, List<CacheMeta>> entries) {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(this.file)))) {
            // write magic and version
            dos.writeInt(0xC053E71C);
            dos.writeByte(0);
            // write keep
            dos.writeByte(keep.size());
            for (ResourceLocation rl : keep) {
                dos.writeUTF(rl.toString());
            }
            // write entries
            dos.writeInt(entries.size());
            for (Map.Entry<String, List<CacheMeta>> entry : entries.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeShort(entry.getValue().size());
                for (CacheMeta meta : entry.getValue()) {
                    dos.writeUTF(meta.subfolder);
                    // if we go over 16 bits there's a bigger problem, but let's support it anyway
                    dos.writeInt(meta.timestamps.size());
                    for (Object2LongMap.Entry<String> timestamp : meta.timestamps.object2LongEntrySet()) {
                        dos.writeUTF(timestamp.getKey());
                        dos.writeLong(timestamp.getLongValue());
                    }
                }
            }
        } catch (IOException e) {
            Logging.getInstance().warn("(Cosmetica Core) Failed to save image cache metadata", e);
        }
    }

    private void read(List<ResourceLocation> keep, Map<String, List<CacheMeta>> entries) throws IOException {
        try (DataInputStream is = new DataInputStream(new BufferedInputStream(Files.newInputStream(this.file)))) {
            // read magic and version
            if (is.readInt() != 0xC053E71C) {
                throw new IOException("Corrupted cache metadata (received magic " + is.readInt() + "). Resetting!");
            }
            int v = is.readUnsignedByte();
            if (v > 0) {
                throw new IOException("Unknown cache metadata version " + v);
            }
            // read keep
            int size = is.readUnsignedByte();
            for (int i = 0; i < size; i++) {
                keep.add(new ResourceLocation(is.readUTF()));
            }
            // read entries
            size = is.readInt();
            for (int i = 0; i < size; i++) {
                String group = is.readUTF();
                List<CacheMeta> metas = new ArrayList<>();

                int cacheMetas = is.readUnsignedShort();
                for (int j = 0; j < cacheMetas; j++) {
                    CacheMeta meta = new CacheMeta();
                    meta.subfolder = is.readUTF();

                    int timestamps = is.readInt();
                    for (int k = 0; k < timestamps; k++) {
                        String subfolder = is.readUTF();
                        long timestamp = is.readLong();
                        meta.timestamps.put(subfolder, timestamp);
                    }

                    metas.add(meta);
                }

                entries.put(group, metas);
            }
        }
    }

    public void runCacheGC() {
    }

    private static class CacheMeta {
        String subfolder;
        final Object2LongMap<String> timestamps = new Object2LongArrayMap<>();
    }
}
