package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TntZoneManager {

    private final FFACore plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<String, TntZone> zones = new LinkedHashMap<>();

    // Per-player selection
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public TntZoneManager(FFACore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tntzones.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        ConfigurationSection sec = data.getConfigurationSection("zones");
        if (sec == null) return;
        for (String name : sec.getKeys(false)) {
            ConfigurationSection z = sec.getConfigurationSection(name);
            if (z == null) continue;
            World world = Bukkit.getWorld(z.getString("world", "world"));
            if (world == null) continue;
            zones.put(name.toLowerCase(), new TntZone(name, world,
                z.getInt("x1"), z.getInt("y1"), z.getInt("z1"),
                z.getInt("x2"), z.getInt("y2"), z.getInt("z2")));
        }
        plugin.getLogger().info("Loaded " + zones.size() + " TNT zones");
    }

    public void save() {
        data = new YamlConfiguration();
        for (TntZone zone : zones.values()) {
            String path = "zones." + zone.name;
            data.set(path + ".world", zone.world.getName());
            data.set(path + ".x1", zone.x1);
            data.set(path + ".y1", zone.y1);
            data.set(path + ".z1", zone.z1);
            data.set(path + ".x2", zone.x2);
            data.set(path + ".y2", zone.y2);
            data.set(path + ".z2", zone.z2);
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save TNT zones: " + e.getMessage());
        }
    }

    public void setPos1(UUID uuid, Location loc) { pos1.put(uuid, loc); }
    public void setPos2(UUID uuid, Location loc) { pos2.put(uuid, loc); }
    public Location getPos1(UUID uuid) { return pos1.get(uuid); }
    public Location getPos2(UUID uuid) { return pos2.get(uuid); }

    public void createZone(String name, Location p1, Location p2) {
        zones.put(name.toLowerCase(), new TntZone(name, p1.getWorld(),
            Math.min(p1.getBlockX(), p2.getBlockX()),
            Math.min(p1.getBlockY(), p2.getBlockY()),
            Math.min(p1.getBlockZ(), p2.getBlockZ()),
            Math.max(p1.getBlockX(), p2.getBlockX()),
            Math.max(p1.getBlockY(), p2.getBlockY()),
            Math.max(p1.getBlockZ(), p2.getBlockZ())));
        save();
    }

    public boolean deleteZone(String name) {
        if (zones.remove(name.toLowerCase()) != null) {
            save();
            return true;
        }
        return false;
    }

    public Collection<TntZone> getZones() { return zones.values(); }

    public boolean isTntBlocked(Location loc) {
        for (TntZone zone : zones.values()) {
            if (zone.contains(loc)) return true;
        }
        return false;
    }

    public static class TntZone {
        public final String name;
        public final World world;
        public final int x1, y1, z1, x2, y2, z2;

        public TntZone(String name, World world, int x1, int y1, int z1, int x2, int y2, int z2) {
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
    }
}
