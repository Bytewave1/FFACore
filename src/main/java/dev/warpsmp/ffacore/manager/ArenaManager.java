package dev.warpsmp.ffacore.manager;
import dev.warpsmp.ffacore.util.Scheduler;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {

    private final FFACore plugin;
    private final File file;
    private final File snapshotsDir;
    private YamlConfiguration data;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Set<LocationKey> placedBlocks = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> snapshotCache = new ConcurrentHashMap<>();

    public ArenaManager(FFACore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        this.snapshotsDir = new File(plugin.getDataFolder(), "snapshots");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        if (!snapshotsDir.exists()) snapshotsDir.mkdirs();
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
        startResetTask();
    }

    private void load() {
        ConfigurationSection section = data.getConfigurationSection("arenas");
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            ConfigurationSection a = section.getConfigurationSection(name);
            if (a == null) continue;
            String worldName = a.getString("world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;
            Arena arena = new Arena(name, world,
                a.getInt("x1"), a.getInt("y1"), a.getInt("z1"),
                a.getInt("x2"), a.getInt("y2"), a.getInt("z2"));
            arenas.put(name.toLowerCase(), arena);
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas");
        loadSnapshotCaches();
    }

    private void loadSnapshotCaches() {
        for (Arena arena : arenas.values()) {
            File snapFile = new File(snapshotsDir, arena.name.toLowerCase() + ".dat");
            if (!snapFile.exists()) continue;
            Set<String> keys = ConcurrentHashMap.newKeySet();
            try (BufferedReader reader = new BufferedReader(new FileReader(snapFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String value = line.substring(eq + 1);
                        // Only cache non-air blocks as protected
                        if (!value.startsWith("minecraft:air") && !value.startsWith("minecraft:cave_air")) {
                            keys.add(line.substring(0, eq));
                        }
                    }
                }
            } catch (IOException ignored) {}
            snapshotCache.put(arena.name.toLowerCase(), keys);
            plugin.getLogger().info("Loaded snapshot cache for " + arena.name + ": " + keys.size() + " protected blocks");
        }
    }

    public void save() {
        data = new YamlConfiguration();
        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.name;
            data.set(path + ".world", arena.world.getName());
            data.set(path + ".x1", arena.x1);
            data.set(path + ".y1", arena.y1);
            data.set(path + ".z1", arena.z1);
            data.set(path + ".x2", arena.x2);
            data.set(path + ".y2", arena.y2);
            data.set(path + ".z2", arena.z2);
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save arenas: " + e.getMessage());
        }
    }

    public void createArena(String name, Location pos1, Location pos2) {
        Arena arena = new Arena(name, pos1.getWorld(),
            Math.min(pos1.getBlockX(), pos2.getBlockX()),
            Math.min(pos1.getBlockY(), pos2.getBlockY()),
            Math.min(pos1.getBlockZ(), pos2.getBlockZ()),
            Math.max(pos1.getBlockX(), pos2.getBlockX()),
            Math.max(pos1.getBlockY(), pos2.getBlockY()),
            Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
        arenas.put(name.toLowerCase(), arena);
        save();
        // Save snapshot of current blocks
        saveSnapshot(arena);
    }

    private void saveSnapshot(Arena arena) {
        // Collect block data on the region thread
        Map<String, String> blocks = new HashMap<>();
        for (int x = arena.x1; x <= arena.x2; x++) {
            for (int y = arena.y1; y <= arena.y2; y++) {
                for (int z = arena.z1; z <= arena.z2; z++) {
                    Block block = arena.world.getBlockAt(x, y, z);
                    blocks.put(x + "," + y + "," + z, block.getBlockData().getAsString());
                }
            }
        }
        // Update cache — only non-air blocks are "original"
        Set<String> nonAirKeys = ConcurrentHashMap.newKeySet();
        for (Map.Entry<String, String> entry : blocks.entrySet()) {
            if (!entry.getValue().startsWith("minecraft:air") && !entry.getValue().startsWith("minecraft:cave_air")) {
                nonAirKeys.add(entry.getKey());
            }
        }
        snapshotCache.put(arena.name.toLowerCase(), nonAirKeys);

        // Write to file async (plain text, not YAML — much faster)
        int count = blocks.size();
        Scheduler.runAsync(plugin, () -> {
            File snapFile = new File(snapshotsDir, arena.name.toLowerCase() + ".dat");
            try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(snapFile))) {
                for (Map.Entry<String, String> entry : blocks.entrySet()) {
                    writer.write(entry.getKey());
                    writer.write('=');
                    writer.write(entry.getValue());
                    writer.newLine();
                }
                plugin.getLogger().info("Snapshot saved for arena " + arena.name + ": " + count + " blocks");
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save snapshot: " + e.getMessage());
            }
        });
    }

    public boolean deleteArena(String name) {
        if (arenas.remove(name.toLowerCase()) != null) {
            save();
            File snapFile = new File(snapshotsDir, name.toLowerCase() + ".dat");
            if (snapFile.exists()) snapFile.delete();
            return true;
        }
        return false;
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public boolean isInAnyArena(Location loc) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(loc)) return true;
        }
        return false;
    }

    /**
     * Checks if a block at this location is an original arena block (in the snapshot).
     */
    public boolean isOriginalBlock(Location loc) {
        for (Arena arena : arenas.values()) {
            if (!arena.contains(loc)) continue;
            Set<String> keys = snapshotCache.get(arena.name.toLowerCase());
            if (keys == null) return false;
            String key = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            if (!keys.contains(key)) return false;
            // Key exists in snapshot = it's an original block (not air originally means protected)
            return true;
        }
        return false;
    }

    public void trackBlock(Location loc) {
        if (isInAnyArena(loc)) {
            placedBlocks.add(new LocationKey(loc));
        }
    }

    /**
     * Resets all arenas to their original snapshot state.
     * Compares every block with the snapshot and restores differences.
     */
    public int resetAllArenas() {
        if (arenas.isEmpty()) {
            plugin.getLogger().warning("No arenas defined! Use /arenareset create first.");
            return 0;
        }
        int totalRestored = 0;
        for (Arena arena : arenas.values()) {
            File snapFile = new File(snapshotsDir, arena.name.toLowerCase() + ".dat");
            if (!snapFile.exists() || snapFile.length() == 0) {
                plugin.getLogger().warning("No snapshot for arena '" + arena.name + "'! Use /arenareset create first.");
                continue;
            }
            totalRestored += resetArena(arena);
        }
        placedBlocks.clear();
        if (totalRestored > 0) {
            plugin.getLogger().info("Arena reset: restored " + totalRestored + " blocks");
        }
        return totalRestored;
    }

    private int resetArena(Arena arena) {
        File snapFile = new File(snapshotsDir, arena.name.toLowerCase() + ".dat");
        if (!snapFile.exists()) {
            plugin.getLogger().warning("No snapshot for arena " + arena.name);
            return 0;
        }

        // Load snapshot async, then restore in batches on region thread
        plugin.getLogger().info("Arena reset " + arena.name + ": loading snapshot...");

        Thread resetThread = new Thread(() -> {
            // Load snapshot
            List<String[]> entries = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(snapFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int eq = line.indexOf('=');
                    if (eq < 0) continue;
                    entries.add(new String[]{line.substring(0, eq), line.substring(eq + 1)});
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load snapshot: " + e.getMessage());
                return;
            }

            plugin.getLogger().info("Arena reset " + arena.name + ": " + entries.size() + " blocks loaded, restoring...");

            // Process in small batches with sleep between to avoid lag
            int batchSize = 200;
            int restored = 0;

            for (int i = 0; i < entries.size(); i += batchSize) {
                int start = i;
                int end = Math.min(i + batchSize, entries.size());

                // Get first location for region scheduler
                String[] firstParts = entries.get(start)[0].split(",");
                Location batchLoc = new Location(arena.world,
                    Integer.parseInt(firstParts[0]),
                    Integer.parseInt(firstParts[1]),
                    Integer.parseInt(firstParts[2]));

                List<String[]> batch = entries.subList(start, end);

                Scheduler.runAtLocation(plugin, batchLoc, () -> {
                    for (String[] entry : batch) {
                        try {
                            String[] parts = entry[0].split(",");
                            int x = Integer.parseInt(parts[0]);
                            int y = Integer.parseInt(parts[1]);
                            int z = Integer.parseInt(parts[2]);
                            Block block = arena.world.getBlockAt(x, y, z);
                            String originalData = entry[1];
                            if (!block.getBlockData().getAsString().equals(originalData)) {
                                block.setBlockData(Bukkit.createBlockData(originalData));
                            }
                        } catch (Exception ignored) {}
                    }
                });

                restored += (end - start);

                // Sleep 50ms between batches to spread load
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
            }

            plugin.getLogger().info("Arena reset " + arena.name + ": done! " + restored + " blocks checked.");
        }, "FFACore-ArenaReset-" + arena.name);
        resetThread.setDaemon(true);
        resetThread.start();

        return 1; // returns immediately, reset happens async
    }

    private void startResetTask() {
        Thread thread = new Thread(() -> {
            while (plugin.isEnabled()) {
                try {
                    Thread.sleep(plugin.getConfig().getLong("arena-reset-interval", 300) * 1000L);
                    if (!arenas.isEmpty()) {
                        resetAllArenas();
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        }, "FFACore-ArenaReset");
        thread.setDaemon(true);
        thread.start();
    }

    public static class Arena {
        public final String name;
        public final World world;
        public final int x1, y1, z1, x2, y2, z2;

        public Arena(String name, World world, int x1, int y1, int z1, int x2, int y2, int z2) {
            this.name = name;
            this.world = world;
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
        }

        public boolean contains(Location loc) {
            if (!loc.getWorld().equals(world)) return false;
            int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
            return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
        }

        public int volume() {
            return (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
        }
    }

    public static class LocationKey {
        public final String world;
        public final int x, y, z;

        public LocationKey(Location loc) {
            this.world = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocationKey k)) return false;
            return x == k.x && y == k.y && z == k.z && world.equals(k.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, y, z);
        }
    }
}
