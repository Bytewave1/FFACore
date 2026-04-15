package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private YamlConfiguration config;
    private final Map<Integer, ShopItem> shopItems = new HashMap<>();
    private String title;
    private int size;

    public ShopManager(FFACore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "shop.yml"));
        shopItems.clear();
        title = config.getString("title", "<bold>Shop</bold>");
        size = config.getInt("size", 27);

        ConfigurationSection items = config.getConfigurationSection("items");
        if (items == null) return;

        for (String key : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(key);
            if (section == null) continue;

            ShopItem item = new ShopItem();
            item.key = key;
            item.material = Material.matchMaterial(section.getString("material", "STONE"));
            item.name = section.getString("name", key);
            item.lore = section.getStringList("lore");
            item.slot = section.getInt("slot", 0);
            item.price = section.getInt("price", 100);
            item.type = section.getString("type", "effect");

            if ("effect".equals(item.type)) {
                String effectName = section.getString("effect", "STRENGTH");
                item.effectType = PotionEffectType.getByName(effectName);
                item.amplifier = section.getInt("amplifier", 0);
                item.duration = section.getInt("duration", 300);
            } else if ("items".equals(item.type)) {
                item.giveItems = new ArrayList<>();
                for (String entry : section.getStringList("items")) {
                    String[] parts = entry.split(":");
                    Material mat = Material.matchMaterial(parts[0]);
                    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    if (mat != null) {
                        item.giveItems.add(new ItemStack(mat, amount));
                    }
                }
            }

            shopItems.put(item.slot, item);
        }
    }

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, size, mm.deserialize(title));

        // Fill with glass panes
        Material fillerMat = Material.matchMaterial(config.getString("filler.material", "GRAY_STAINED_GLASS_PANE"));
        if (fillerMat == null) fillerMat = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(mm.deserialize(config.getString("filler.name", " ")));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        int coins = plugin.getCoinManager().getCoins(player.getUniqueId());

        // Place shop items
        for (ShopItem item : shopItems.values()) {
            if (item.material == null) continue;
            ItemStack display = new ItemStack(item.material);
            ItemMeta meta = display.getItemMeta();

            meta.displayName(mm.deserialize(item.name));

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : item.lore) {
                String parsed = line
                    .replace("{price}", String.valueOf(item.price))
                    .replace("{duration}", String.valueOf(item.duration));
                lore.add(mm.deserialize(parsed));
            }
            // Add afford indicator
            if (coins >= item.price) {
                lore.add(mm.deserialize(""));
                lore.add(mm.deserialize("<green>ʏᴏᴜ ᴄᴀɴ ᴀғғᴏʀᴅ ᴛʜɪs</green>"));
            } else {
                lore.add(mm.deserialize(""));
                lore.add(mm.deserialize("<red>ɴᴏᴛ ᴇɴᴏᴜɢʜ ᴄᴏɪɴs</red> <dark_gray>(" + coins + "/" + item.price + ")</dark_gray>"));
            }
            meta.setLore(null); // clear legacy lore
            meta.lore(lore);
            display.setItemMeta(meta);

            inv.setItem(item.slot, display);
        }

        player.openInventory(inv);
    }

    public ShopItem getItem(int slot) {
        return shopItems.get(slot);
    }

    public boolean purchase(Player player, ShopItem item) {
        CoinManager cm = plugin.getCoinManager();
        if (!cm.removeCoins(player.getUniqueId(), item.price)) return false;

        if ("effect".equals(item.type) && item.effectType != null) {
            player.addPotionEffect(new PotionEffect(item.effectType, item.duration * 20, item.amplifier, false, true, true));
        } else if ("items".equals(item.type) && item.giveItems != null) {
            for (ItemStack give : item.giveItems) {
                player.getInventory().addItem(give.clone());
            }
        }

        cm.save();
        return true;
    }

    public String getTitle() { return title; }

    public static class ShopItem {
        public String key;
        public Material material;
        public String name;
        public List<String> lore;
        public int slot;
        public int price;
        public String type;
        public PotionEffectType effectType;
        public int amplifier;
        public int duration;
        public List<ItemStack> giveItems;
    }
}
