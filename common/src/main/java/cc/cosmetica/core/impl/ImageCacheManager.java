package cc.cosmetica.core.impl;

import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
    public ImageCacheManager(Path root) throws IOException {
        this.root = root;
        this.file = root.resolve("imagecachemanager");

        if (Files.exists(this.file)) {
            this.read(this.keep, this.entries);
        }
    }

    private final Path root, file;
    private List<ResourceLocation> keep = new ArrayList<>(); // max size 256
    private final Map<String, CacheMeta> entries = new HashMap<>();

    /**
     * Mark and track the given entry for disk management.
     * @param group the path group to track.
     */
    public void mark(Path group, String subfolder) {
        this.entries.computeIfAbsent(this.root.relativize(group).toString(), g -> new CacheMeta())
                .timestamps.put(subfolder, Instant.now().toEpochMilli());
    }

    public void saveSync() throws IOException {
        Logging.getInstance().debug("Saving cosmetica image cache metadata (sync)");
        this.save(this.keep, this.entries);
    }

    public void saveAsync() throws IOException {
        // gather data on main thread, save off thread
        Logging.getInstance().debug("Saving cosmetica image cache metadata (async)");
        long timestamp = System.nanoTime();

        // deep copy
        final Map<String, CacheMeta> saveEntries = new HashMap<>();
        entries.forEach((s,meta)->{
            CacheMeta cloneMeta = new CacheMeta();
            cloneMeta.timestamps.putAll(meta.timestamps);
            saveEntries.put(s, cloneMeta);
        });

        long dt = (System.nanoTime() - timestamp) / 1_000_000;
        if (dt >= 10) { // 50ms in a tick. should be much less!
            Logging.getInstance().warn("(Cosmetica Core) Image cache metadata clone took {} ms!", dt);
        }

        final List<ResourceLocation> keep = new ArrayList<>(this.keep);
        // launch save async
        CompletableFuture.runAsync(() -> this.save(keep, saveEntries), Util.backgroundExecutor());
    }

    private void save(List<ResourceLocation> keep, Map<String, CacheMeta> entries) {
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
            for (Map.Entry<String, CacheMeta> entry : entries.entrySet()) {
                dos.writeUTF(entry.getKey()); // group
                entry.getValue().store(dos);  // meta
            }
        } catch (IOException e) {
            Logging.getInstance().warn("(Cosmetica Core) Failed to save image cache metadata", e);
        }
    }

    private void read(List<ResourceLocation> keep, Map<String, CacheMeta> entries) throws IOException {
        long time = System.nanoTime();

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

                CacheMeta meta = new CacheMeta();
                meta.load(is);

                entries.put(group, meta);
            }
        }

        if ((System.nanoTime() - time)/1_000_000 > 100) {
            Logging.getInstance().warn("(Cosmetica Core) Loading image cache metadata took over 100ms.");
        }
    }

    public void runCacheGC() {
    }

    private static class CacheMeta {
        final Object2LongMap<String> timestamps = new Object2LongArrayMap<>();

        void load(DataInputStream is) throws IOException {
            int timestamps = is.readInt();
            for (int k = 0; k < timestamps; k++) {
                String subfolder = is.readUTF();
                long timestamp = is.readLong();
                this.timestamps.put(subfolder, timestamp);
            }
        }

        void store(DataOutputStream dos) throws IOException {
            // if we go over 16 bits there's a bigger problem, but let's support it anyway
            dos.writeInt(this.timestamps.size());

            for (Object2LongMap.Entry<String> timestamp : this.timestamps.object2LongEntrySet()) {
                dos.writeUTF(timestamp.getKey());        // subfolder
                dos.writeLong(timestamp.getLongValue()); // timestamp
            }
        }
    }
}
