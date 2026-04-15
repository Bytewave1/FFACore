package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {

    private final FFACore plugin;
    private final File defaultFile;
    private final File playerKitsDir;
    private YamlConfiguration defaultData;

    public KitManager(FFACore plugin) {
        this.plugin = plugin;
        this.defaultFile = new File(plugin.getDataFolder(), "kit.yml");
        this.playerKitsDir = new File(plugin.getDataFolder(), "playerkits");
        if (!defaultFile.exists()) {
            try { defaultFile.createNewFile(); } catch (IOException ignored) {}
        }
        if (!playerKitsDir.exists()) {
            playerKitsDir.mkdirs();
        }
        this.defaultData = YamlConfiguration.loadConfiguration(defaultFile);
    }

    // ====== DEFAULT KIT (admin) ======

    public void saveDefaultKit(Player player) {
        defaultData = new YamlConfiguration();
        saveInventoryToConfig(defaultData, player);
        try {
            defaultData.save(defaultFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save default kit: " + e.getMessage());
        }
    }

    public boolean hasDefaultKit() {
        return defaultData.contains("contents") || defaultData.contains("armor");
    }

    public int getDefaultItemCount() {
        return countItems(defaultData);
    }

    // ====== PLAYER KIT ======

    public void savePlayerKit(Player player) {
        File file = new File(playerKitsDir, player.getUniqueId() + ".yml");
        YamlConfiguration data = new YamlConfiguration();
        saveInventoryToConfig(data, player);
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player kit: " + e.getMessage());
        }
    }

    public boolean hasPlayerKit(UUID uuid) {
        File file = new File(playerKitsDir, uuid + ".yml");
        return file.exists();
    }

    public void deletePlayerKit(UUID uuid) {
        File file = new File(playerKitsDir, uuid + ".yml");
        if (file.exists()) file.delete();
    }

    // ====== ITEM VALIDATION ======

    /**
     * Checks if a player's current inventory has the same items as the default kit.
     * Same materials and amounts, slot positions don't matter.
     */
    public boolean matchesDefaultKit(Player player) {
        if (!hasDefaultKit()) return false;

        Map<Material, Integer> defaultItems = getItemMap(defaultData);
        Map<Material, Integer> playerItems = getPlayerItemMap(player);

        return defaultItems.equals(playerItems);
    }

    private Map<Material, Integer> getItemMap(YamlConfiguration data) {
        Map<Material, Integer> items = new HashMap<>();
        if (data.contains("contents")) {
            for (String key : data.getConfigurationSection("contents").getKeys(false)) {
                ItemStack item = data.getItemStack("contents." + key);
                if (item != null && item.getType() != Material.AIR) {
                    items.merge(item.getType(), item.getAmount(), Integer::sum);
                }
            }
        }
        if (data.contains("armor")) {
            for (String key : data.getConfigurationSection("armor").getKeys(false)) {
                ItemStack item = data.getItemStack("armor." + key);
                if (item != null && item.getType() != Material.AIR) {
                    items.merge(item.getType(), item.getAmount(), Integer::sum);
                }
            }
        }
        if (data.contains("offhand")) {
            ItemStack item = data.getItemStack("offhand");
            if (item != null && item.getType() != Material.AIR) {
                items.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        return items;
    }

    private Map<Material, Integer> getPlayerItemMap(Player player) {
        Map<Material, Integer> items = new HashMap<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.AIR) {
            items.merge(offhand.getType(), offhand.getAmount(), Integer::sum);
        }
        return items;
    }

    // ====== GIVE KIT ======

    public void giveKit(Player player) {
        applyKit(player);
        // Verify kit was applied after 5 ticks, re-apply if empty (Folia timing issues)
        player.getScheduler().runDelayed(plugin, t -> {
            if (!player.isOnline() || player.isDead()) return;
            if (player.getInventory().isEmpty()) {
                plugin.getLogger().info("Kit re-apply for " + player.getName() + " (inventory was empty)");
                applyKit(player);
            }
        }, null, 5L);
        // Final safety net at 15 ticks
        player.getScheduler().runDelayed(plugin, t -> {
            if (!player.isOnline() || player.isDead()) return;
            if (player.getInventory().isEmpty()) {
                plugin.getLogger().info("Kit final re-apply for " + player.getName());
                applyKit(player);
            }
        }, null, 15L);
    }

    private void applyKit(Player player) {
        YamlConfiguration data;

        // Check for player-specific kit first
        File playerFile = new File(playerKitsDir, player.getUniqueId() + ".yml");
        if (playerFile.exists()) {
            data = YamlConfiguration.loadConfiguration(playerFile);
        } else {
            data = defaultData;
        }

        if (!data.contains("contents") && !data.contains("armor")) return;

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

        // Contents
        if (data.contains("contents")) {
            for (String key : data.getConfigurationSection("contents").getKeys(false)) {
                int slot = Integer.parseInt(key);
                ItemStack item = data.getItemStack("contents." + key);
                if (item != null) {
                    player.getInventory().setItem(slot, item.clone());
                }
            }
        }

        // Armor
        if (data.contains("armor")) {
            ItemStack[] armor = new ItemStack[4];
            for (String key : data.getConfigurationSection("armor").getKeys(false)) {
                int slot = Integer.parseInt(key);
                ItemStack item = data.getItemStack("armor." + key);
                if (item != null && slot < 4) {
                    armor[slot] = item.clone();
                }
            }
            player.getInventory().setArmorContents(armor);
        }

        // Offhand
        if (data.contains("offhand")) {
            ItemStack offhand = data.getItemStack("offhand");
            if (offhand != null) {
                player.getInventory().setItemInOffHand(offhand.clone());
            }
        }

        // Heal
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.updateInventory();
    }

    // ====== HELPERS ======

    private void saveInventoryToConfig(YamlConfiguration data, Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                data.set("contents." + i, contents[i]);
            }
        }
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && armor[i].getType() != Material.AIR) {
                data.set("armor." + i, armor[i]);
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.AIR) {
            data.set("offhand", offhand);
        }
    }

    private int countItems(YamlConfiguration data) {
        int count = 0;
        if (data.contains("contents")) count += data.getConfigurationSection("contents").getKeys(false).size();
        if (data.contains("armor")) count += data.getConfigurationSection("armor").getKeys(false).size();
        if (data.contains("offhand")) count++;
        return count;
    }

    public boolean hasKit() {
        return hasDefaultKit();
    }

    public int getItemCount() {
        return getDefaultItemCount();
    }
}
