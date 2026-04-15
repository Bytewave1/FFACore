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
    private YamlConfiguration data;
    private Location spawn;
    private boolean loaded = false;

    public SpawnManager(FFACore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    private void tryLoad() {
        if (loaded || !data.contains("world")) return;
        World world = Bukkit.getWorld(data.getString("world"));
        if (world == null) return;
        spawn = new Location(world,
            data.getDouble("x"),
            data.getDouble("y"),
            data.getDouble("z"),
            (float) data.getDouble("yaw"),
            (float) data.getDouble("pitch"));
        loaded = true;
        plugin.getLogger().info("Spawn loaded: " + spawn.getWorld().getName() +
            " " + spawn.getBlockX() + " " + spawn.getBlockY() + " " + spawn.getBlockZ());
    }

    public void setSpawn(Location loc) {
        this.spawn = loc.clone();
        this.loaded = true;
        data = new YamlConfiguration();
        data.set("world", loc.getWorld().getName());
        data.set("x", loc.getX());
        data.set("y", loc.getY());
        data.set("z", loc.getZ());
        data.set("yaw", (double) loc.getYaw());
        data.set("pitch", (double) loc.getPitch());
        try {
            data.save(file);
            plugin.getLogger().info("Spawn saved: " + loc.getWorld().getName() +
                " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() +
                " (file: " + file.getAbsolutePath() + ")");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save spawn: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Location getSpawn() {
        tryLoad();
        return spawn;
    }

    public boolean hasSpawn() {
        tryLoad();
        return spawn != null;
    }
}
