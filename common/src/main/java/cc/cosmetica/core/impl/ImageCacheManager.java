/*
 * Copyright 2024, 2025 Cosmetica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.cosmetica.core.impl;

import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.Util;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ImageCacheManager {
    /**
     * Create and load a new cache manager.
     */
    public ImageCacheManager(Path root) {
        this.root = root;
        this.file = root.resolve("imagecachemanager");

        if (Files.exists(this.file)) {
            try {
                this.read(this.keep, this.entries);
            } catch (IOException e) {
                Logging.getInstance().error("Failed to load image cache metadata!", e);
            }
        }
    }

    private final Path root, file;
    private List<String> keep = new ArrayList<>(); // max size 256. group+/+subfolder
    private final Map<String, CacheMeta> entries = new HashMap<>();

    /**
     * Mark and track the given entry for disk management. Sets last accessed to now.
     * @param group the path group to track.
     * @param subdirectory the subdirectory to track.
     */
    void mark(Path group, String subdirectory) {
        this.entries.computeIfAbsent(this.root.relativize(group).toString(), g -> new CacheMeta())
                .timestamps.put(subdirectory, Instant.now().toEpochMilli());
    }

    void setKeep(List<String> toKeep) {
        if (toKeep.size() > 256) {
            throw new IllegalArgumentException("Cannot force-preserve more than 256 cosmetics in cache.");
        }

        this.keep = toKeep;
    }

    public void saveSync() throws IOException {
        Logging.getInstance().debug(LoggingCategories.ASSETS, "Saving cosmetica image cache metadata (sync)");
        this.save(this.keep, this.entries);
    }

    public void saveAsync() {
        // gather data on main thread, save off thread
        Logging.getInstance().debug(LoggingCategories.ASSETS, "Saving cosmetica image cache metadata (async)");
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

        final List<String> keep = new ArrayList<>(this.keep);
        // launch save async
        CompletableFuture.runAsync(() -> this.save(keep, saveEntries), Util.backgroundExecutor());
    }

    private void save(List<String> keep, Map<String, CacheMeta> entries) {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(this.file)))) {
            // write magic and version
            dos.writeInt(0xC053E71C);
            dos.writeByte(0);
            // write keep
            dos.writeByte(keep.size());
            for (String rl : keep) {
                dos.writeUTF(rl);
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

    private void read(List<String> keep, Map<String, CacheMeta> entries) throws IOException {
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
                keep.add(is.readUTF());
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

    public void clearOldEntries() {
        Logging.getInstance().debug(LoggingCategories.ASSETS, "Clearing old cache entries");

        // 14 day cache
        long now = System.nanoTime();
        long exp = Instant.now().minus(14, ChronoUnit.DAYS).toEpochMilli();

        Iterator<Map.Entry<String, CacheMeta>> it = this.entries.entrySet().iterator();
        int count = 0;

        while (it.hasNext()) {
            Map.Entry<String, CacheMeta> entry = it.next();
            String groupPre = entry.getKey() + "/";
            CacheMeta meta = entry.getValue();

            Iterator<Object2LongMap.Entry<String>> subfolderIt = meta.timestamps.object2LongEntrySet().iterator();

            while (subfolderIt.hasNext()) {
                Object2LongMap.Entry<String> timestamp = subfolderIt.next();

                if (timestamp.getLongValue() - exp < 0) { // before the Expiry date
                    String subfolder = timestamp.getKey();
                    String path = groupPre + subfolder;
                    Path p;

                    if (!keep.contains(path)) {
                        if (Files.exists(p = this.root.resolve(path))) {
                            // remove file
                            try {
                                FileUtils.deleteDirectory(p.toFile());
                                subfolderIt.remove();
                                count++;
                            } catch (IOException e) {
                                Logging.getInstance().error("Failed to delete {} from cache", e, path);
                            }
                        } else {
                            // file is already gone
                            subfolderIt.remove();
                            count++;
                        }
                    }
                }
            }

            // remove empty groups from cache manager
            if (meta.timestamps.isEmpty()) {
                it.remove();
            }
        }

        long delay = (System.nanoTime() - now) / 1_000_000;
        Logging.getInstance().debug(LoggingCategories.ASSETS, "Cleared {} old cache entries in {} ms", count, delay);
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
