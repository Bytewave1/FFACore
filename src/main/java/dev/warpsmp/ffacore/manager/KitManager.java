package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KitManager {

    private final FFACore plugin;
    private final File file;
    private YamlConfiguration data;

    public KitManager(FFACore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kit.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void saveKit(Player player) {
        data.set("contents", null);
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                data.set("contents." + i, contents[i]);
            }
        }
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) {
                data.set("armor." + i, armor[i]);
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != org.bukkit.Material.AIR) {
            data.set("offhand", offhand);
        } else {
            data.set("offhand", null);
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save kit: " + e.getMessage());
        }
    }

    public boolean hasKit() {
        return data.contains("contents") || data.contains("armor");
    }

    public int getItemCount() {
        int count = 0;
        if (data.contains("contents")) count += data.getConfigurationSection("contents").getKeys(false).size();
        if (data.contains("armor")) count += data.getConfigurationSection("armor").getKeys(false).size();
        if (data.contains("offhand")) count++;
        return count;
    }

    public void giveKit(Player player) {
        if (!hasKit()) return;

        if (plugin.getConfig().getBoolean("clear-before-kit", true)) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
        }

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

        // Heal player
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
    }
}
