package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CoinManager {

    private final FFACore plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, Integer> coins = new ConcurrentHashMap<>();

    public CoinManager(FFACore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "coins.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        for (String key : data.getKeys(false)) {
            try {
                coins.put(UUID.fromString(key), data.getInt(key));
            } catch (Exception ignored) {}
        }
    }

    public int getCoins(UUID uuid) {
        return coins.getOrDefault(uuid, 0);
    }

    public void setCoins(UUID uuid, int amount) {
        coins.put(uuid, Math.max(0, amount));
    }

    public void addCoins(UUID uuid, int amount) {
        coins.put(uuid, getCoins(uuid) + amount);
    }

    public boolean removeCoins(UUID uuid, int amount) {
        int current = getCoins(uuid);
        if (current < amount) return false;
        coins.put(uuid, current - amount);
        return true;
    }

    public void save() {
        for (Map.Entry<UUID, Integer> entry : coins.entrySet()) {
            data.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save coins: " + e.getMessage());
        }
    }
}
