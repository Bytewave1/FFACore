package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.util.Scheduler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AfkZoneManager {

    private final FFACore plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<String, AfkZone> zones = new LinkedHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Per-player selection
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    // Per-player AFK tracking
    private final Map<UUID, Long> afkStartTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> afkEarned = new ConcurrentHashMap<>();

    public AfkZoneManager(FFACore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "afkzones.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
        startAfkTask();
    }

    private void load() {
        ConfigurationSection sec = data.getConfigurationSection("zones");
        if (sec == null) return;
        for (String name : sec.getKeys(false)) {
            ConfigurationSection z = sec.getConfigurationSection(name);
            if (z == null) continue;
            World world = Bukkit.getWorld(z.getString("world", "world"));
            if (world == null) continue;
            zones.put(name.toLowerCase(), new AfkZone(name, world,
                z.getInt("x1"), z.getInt("y1"), z.getInt("z1"),
                z.getInt("x2"), z.getInt("y2"), z.getInt("z2")));
        }
        plugin.getLogger().info("Loaded " + zones.size() + " AFK zones");
    }

    public void save() {
        data = new YamlConfiguration();
        for (AfkZone zone : zones.values()) {
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
            plugin.getLogger().warning("Failed to save AFK zones: " + e.getMessage());
        }
    }

    public void setPos1(UUID uuid, Location loc) { pos1.put(uuid, loc); }
    public void setPos2(UUID uuid, Location loc) { pos2.put(uuid, loc); }
    public Location getPos1(UUID uuid) { return pos1.get(uuid); }
    public Location getPos2(UUID uuid) { return pos2.get(uuid); }

    public void createZone(String name, Location p1, Location p2) {
        zones.put(name.toLowerCase(), new AfkZone(name, p1.getWorld(),
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

    public Collection<AfkZone> getZones() { return zones.values(); }

    public boolean isInAfkZone(Location loc) {
        for (AfkZone zone : zones.values()) {
            if (zone.contains(loc)) return true;
        }
        return false;
    }

    private void startAfkTask() {
        Thread thread = new Thread(() -> {
            while (plugin.isEnabled()) {
                try {
                    Thread.sleep(1000);
                    if (zones.isEmpty()) continue;

                    int coinsPerMinute = plugin.getConfig().getInt("afk-coins-per-minute", 20);

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        UUID uuid = player.getUniqueId();
                        boolean inZone = isInAfkZone(player.getLocation());

                        if (inZone) {
                            // Start tracking if not already
                            if (!afkStartTime.containsKey(uuid)) {
                                afkStartTime.put(uuid, System.currentTimeMillis());
                                afkEarned.put(uuid, 0);
                            }

                            long elapsed = System.currentTimeMillis() - afkStartTime.get(uuid);
                            long totalSeconds = elapsed / 1000;
                            long minutes = totalSeconds / 60;
                            long seconds = totalSeconds % 60;

                            // Award coins every 60 seconds
                            int shouldHaveEarned = (int) (minutes * coinsPerMinute);
                            int currentEarned = afkEarned.getOrDefault(uuid, 0);
                            if (shouldHaveEarned > currentEarned) {
                                int toAdd = shouldHaveEarned - currentEarned;
                                plugin.getCoinManager().addCoins(uuid, toAdd);
                                afkEarned.put(uuid, shouldHaveEarned);
                            }

                            // Next payout in seconds
                            long nextPayout = 60 - seconds;
                            int nextAmount = coinsPerMinute;

                            String actionBar = plugin.getMessageManager().getRaw("afk-earning")
                                .replace("{time}", String.format("%dm %02ds", minutes, seconds))
                                .replace("{earned}", String.valueOf(currentEarned))
                                .replace("{next}", String.valueOf(nextPayout))
                                .replace("{amount}", String.valueOf(nextAmount));

                            Scheduler.runPlayer(plugin, player, () -> {
                                player.sendActionBar(mm.deserialize(actionBar));
                            });
                        } else {
                            // Left AFK zone
                            if (afkStartTime.containsKey(uuid)) {
                                int earned = afkEarned.getOrDefault(uuid, 0);
                                if (earned > 0) {
                                    plugin.getCoinManager().save();
                                    String msg = plugin.getMessageManager().getRaw("afk-leave")
                                        .replace("{earned}", String.valueOf(earned));
                                    Scheduler.runPlayer(plugin, player, () -> {
                                        player.sendMessage(mm.deserialize(msg));
                                    });
                                }
                                afkStartTime.remove(uuid);
                                afkEarned.remove(uuid);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }, "FFACore-AfkTracker");
        thread.setDaemon(true);
        thread.start();
    }

    public void removePlayer(UUID uuid) {
        afkStartTime.remove(uuid);
        afkEarned.remove(uuid);
    }

    public static class AfkZone {
        public final String name;
        public final World world;
        public final int x1, y1, z1, x2, y2, z2;

        public AfkZone(String name, World world, int x1, int y1, int z1, int x2, int y2, int z2) {
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
