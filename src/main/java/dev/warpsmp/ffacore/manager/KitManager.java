package dev.warpsmp.ffacore.manager;
import dev.warpsmp.ffacore.util.Scheduler;

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
    private final File playerKitsDir;
    private final File adminKitsDir;

    public KitManager(FFACore plugin) {
        this.plugin = plugin;
        this.playerKitsDir = new File(plugin.getDataFolder(), "playerkits");
        this.adminKitsDir = new File(plugin.getDataFolder(), "admin-kits");
        if (!playerKitsDir.exists()) {
            playerKitsDir.mkdirs();
        }
        if (!adminKitsDir.exists()) {
            adminKitsDir.mkdirs();
        }
    }

    // ====== ADMIN KIT (numbered) ======

    public void saveAdminKit(int kitNumber, Player player) {
        File file = new File(adminKitsDir, "kit-" + kitNumber + ".yml");
        YamlConfiguration data = new YamlConfiguration();
        saveInventoryToConfig(data, player);
        try {
            data.save(file);
            plugin.getLogger().info("Admin kit #" + kitNumber + " saved");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save admin kit #" + kitNumber + ": " + e.getMessage());
        }
    }

    public boolean hasAdminKit(int kitNumber) {
        File file = new File(adminKitsDir, "kit-" + kitNumber + ".yml");
        if (!file.exists()) return false;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        return data.contains("contents") || data.contains("armor");
    }

    public List<Integer> getAllAdminKitNumbers() {
        List<Integer> kits = new ArrayList<>();
        if (!adminKitsDir.exists()) return kits;
        File[] files = adminKitsDir.listFiles((dir, name) -> name.startsWith("kit-") && name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    String name = file.getName();
                    int kitNum = Integer.parseInt(name.substring(4, name.length() - 4));
                    kits.add(kitNum);
                } catch (NumberFormatException ignored) {}
            }
        }
        Collections.sort(kits);
        return kits;
    }

    public YamlConfiguration getAdminKit(int kitNumber) {
        File file = new File(adminKitsDir, "kit-" + kitNumber + ".yml");
        if (!file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    public int getRandomAdminKitNumber() {
        List<Integer> kits = getAllAdminKitNumbers();
        if (kits.isEmpty()) return -1;
        return kits.get(new Random().nextInt(kits.size()));
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
     * Checks if a player's current inventory has the same items as an admin kit.
     */
    public boolean matchesAdminKit(Player player, int kitNumber) {
        YamlConfiguration data = getAdminKit(kitNumber);
        if (data == null) return false;

        Map<Material, Integer> kitItems = getItemMap(data);
        Map<Material, Integer> playerItems = getPlayerItemMap(player);

        return kitItems.equals(playerItems);
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
        applyKit(player, -1);
        // Verify kit was applied after 5 ticks, re-apply if empty (Folia timing issues)
        Scheduler.runPlayerDelayed(plugin, player, () -> {
            if (!player.isOnline() || player.isDead()) return;
            if (player.getInventory().isEmpty()) {
                plugin.getLogger().info("Kit re-apply for " + player.getName() + " (inventory was empty)");
                applyKit(player, -1);
            }
        }, 5L);
        // Final safety net at 15 ticks
        Scheduler.runPlayerDelayed(plugin, player, () -> {
            if (!player.isOnline() || player.isDead()) return;
            if (player.getInventory().isEmpty()) {
                plugin.getLogger().info("Kit final re-apply for " + player.getName());
                applyKit(player, -1);
            }
        }, 15L);
    }

    public void giveRandomAdminKit(Player player) {
        int kitNum = getRandomAdminKitNumber();
        if (kitNum == -1) {
            // No admin kits available - clear inventory
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setFireTicks(0);
            player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
            return;
        }
        applyKit(player, kitNum);
        // Verify kit was applied after 5 ticks, re-apply if empty
        Scheduler.runPlayerDelayed(plugin, player, () -> {
            if (!player.isOnline() || player.isDead()) return;
            if (player.getInventory().isEmpty()) {
                plugin.getLogger().info("Random kit re-apply for " + player.getName());
                applyKit(player, kitNum);
            }
        }, 5L);
    }

    private void applyKit(Player player, int kitNumber) {
        YamlConfiguration data = null;

        // If kitNumber >= 0, use admin kit
        if (kitNumber >= 0) {
            data = getAdminKit(kitNumber);
        }
        // Otherwise check for player-specific kit
        if (data == null) {
            File playerFile = new File(playerKitsDir, player.getUniqueId() + ".yml");
            if (playerFile.exists()) {
                data = YamlConfiguration.loadConfiguration(playerFile);
            }
        }

        // If still no kit found, just clear inventory
        if (data == null || (!data.contains("contents") && !data.contains("armor"))) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setFireTicks(0);
            player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
            return;
        }

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
}
