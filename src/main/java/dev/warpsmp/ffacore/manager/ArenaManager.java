package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {

    private final FFACore plugin;
    private final File file;
    private final File snapshotsDir;
    private YamlConfiguration data;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Set<LocationKey> placedBlocks = ConcurrentHashMap.newKeySet();

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
        File snapFile = new File(snapshotsDir, arena.name.toLowerCase() + ".dat");
        YamlConfiguration snap = new YamlConfiguration();
        int count = 0;
        for (int x = arena.x1; x <= arena.x2; x++) {
            for (int y = arena.y1; y <= arena.y2; y++) {
                for (int z = arena.z1; z <= arena.z2; z++) {
                    Block block = arena.world.getBlockAt(x, y, z);
                    String key = x + "," + y + "," + z;
                    snap.set(key, block.getBlockData().getAsString());
                    count++;
                }
            }
        }
        try {
            snap.save(snapFile);
            plugin.getLogger().info("Snapshot saved for arena " + arena.name + ": " + count + " blocks");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save snapshot: " + e.getMessage());
        }
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
        int totalRestored = 0;
        for (Arena arena : arenas.values()) {
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
        if (!snapFile.exists()) return 0;

        YamlConfiguration snap = YamlConfiguration.loadConfiguration(snapFile);
        int restored = 0;

        for (String key : snap.getKeys(false)) {
            String[] parts = key.split(",");
            if (parts.length != 3) continue;
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            String originalData = snap.getString(key);
            if (originalData == null) continue;

            Block block = arena.world.getBlockAt(x, y, z);
            String currentData = block.getBlockData().getAsString();

            if (!currentData.equals(originalData)) {
                try {
                    BlockData blockData = Bukkit.createBlockData(originalData);
                    Location loc = new Location(arena.world, x, y, z);
                    Bukkit.getRegionScheduler().run(plugin, loc, task -> {
                        block.setBlockData(blockData);
                    });
                    restored++;
                } catch (Exception ignored) {}
            }
        }
        return restored;
    }

    private void startResetTask() {
        Thread thread = new Thread(() -> {
            while (plugin.isEnabled()) {
                try {
                    Thread.sleep(plugin.getConfig().getLong("arena-reset-interval", 300) * 1000L);
                    if (!arenas.isEmpty()) {
                        Bukkit.getGlobalRegionScheduler().run(plugin, task -> resetAllArenas());
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
        private final String world;
        private final int x, y, z;

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
