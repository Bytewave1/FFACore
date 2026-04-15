package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {

    private final FFACore plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Set<LocationKey> placedBlocks = ConcurrentHashMap.newKeySet();

    public ArenaManager(FFACore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
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
    }

    public boolean deleteArena(String name) {
        if (arenas.remove(name.toLowerCase()) != null) {
            save();
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

    public void resetAllArenas() {
        int count = 0;
        for (LocationKey key : placedBlocks) {
            Location loc = key.toLocation();
            if (loc != null && loc.isChunkLoaded()) {
                Block block = loc.getBlock();
                if (block.getType() != Material.AIR) {
                    Bukkit.getRegionScheduler().run(plugin, loc, task -> {
                        loc.getBlock().setType(Material.AIR);
                    });
                    count++;
                }
            }
        }
        placedBlocks.clear();
        if (count > 0) {
            plugin.getLogger().info("Arena reset: removed " + count + " player-placed blocks");
        }
    }

    private void startResetTask() {
        long intervalTicks = plugin.getConfig().getLong("arena-reset-interval", 300) * 20L;
        Thread thread = new Thread(() -> {
            while (plugin.isEnabled()) {
                try {
                    Thread.sleep(plugin.getConfig().getLong("arena-reset-interval", 300) * 1000L);
                    if (!placedBlocks.isEmpty()) {
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

        public Location toLocation() {
            World w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, x + 0.5, y, z + 0.5);
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
