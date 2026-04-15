package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager {

    private final FFACore plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();

    public StatsManager(FFACore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerStats s = new PlayerStats();
                s.name = data.getString(key + ".name", "???");
                s.kills = data.getInt(key + ".kills", 0);
                s.deaths = data.getInt(key + ".deaths", 0);
                s.highestStreak = data.getInt(key + ".highestStreak", 0);
                stats.put(uuid, s);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String path = entry.getKey().toString();
            PlayerStats s = entry.getValue();
            data.set(path + ".name", s.name);
            data.set(path + ".kills", s.kills);
            data.set(path + ".deaths", s.deaths);
            data.set(path + ".highestStreak", s.highestStreak);
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save stats: " + e.getMessage());
        }
    }

    public PlayerStats getStats(UUID uuid) {
        return stats.computeIfAbsent(uuid, k -> new PlayerStats());
    }

    public void addKill(UUID uuid, String name) {
        PlayerStats s = getStats(uuid);
        s.name = name;
        s.kills++;
    }

    public void addDeath(UUID uuid, String name) {
        PlayerStats s = getStats(uuid);
        s.name = name;
        s.deaths++;
    }

    public void checkHighestStreak(UUID uuid, int streak) {
        PlayerStats s = getStats(uuid);
        if (streak > s.highestStreak) {
            s.highestStreak = streak;
        }
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopKills(int limit) {
        List<Map.Entry<UUID, PlayerStats>> sorted = new ArrayList<>(stats.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue().kills, a.getValue().kills));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    public int getRank(UUID uuid) {
        List<Map.Entry<UUID, PlayerStats>> sorted = new ArrayList<>(stats.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue().kills, a.getValue().kills));
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(uuid)) return i + 1;
        }
        return sorted.size() + 1;
    }

    public static class PlayerStats {
        public String name = "???";
        public int kills = 0;
        public int deaths = 0;
        public int highestStreak = 0;

        public double getKDR() {
            if (deaths == 0) return kills;
            return Math.round((double) kills / deaths * 100.0) / 100.0;
        }
    }
}
