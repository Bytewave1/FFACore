package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SpawnManager {

    private final FFACore plugin;
    private final File file;
    private final YamlConfiguration data;
    private Location spawn;

    public SpawnManager(FFACore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        if (!data.contains("world")) return;
        World world = Bukkit.getWorld(data.getString("world"));
        if (world == null) return;
        spawn = new Location(world,
            data.getDouble("x"),
            data.getDouble("y"),
            data.getDouble("z"),
            (float) data.getDouble("yaw"),
            (float) data.getDouble("pitch"));
    }

    public void setSpawn(Location loc) {
        this.spawn = loc;
        data.set("world", loc.getWorld().getName());
        data.set("x", loc.getX());
        data.set("y", loc.getY());
        data.set("z", loc.getZ());
        data.set("yaw", loc.getYaw());
        data.set("pitch", loc.getPitch());
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save spawn: " + e.getMessage());
        }
    }

    public Location getSpawn() {
        return spawn;
    }

    public boolean hasSpawn() {
        return spawn != null;
    }
}
