package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.*;

public class ShopManager {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private YamlConfiguration config;

    // Menu identifiers stored in inventory title
    public static final String MAIN_ID = "ffashop:main";
    public static final String EFFECTS_ID = "ffashop:effects";
    public static final String CRYSTALS_ID = "ffashop:fighting";
    public static final String AMOUNT_ID = "ffashop:amount";

    // Per-player state
    private final Map<UUID, ShopItem> pendingMultiBuy = new HashMap<>();
    private final Map<UUID, String> openMenus = new HashMap<>();

    public ShopManager(FFACore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "shop.yml"));
    }

    // ====== OPEN MENUS ======

    public void openMainMenu(Player player) {
        ConfigurationSection sec = config.getConfigurationSection("main-menu");
        if (sec == null) return;
        Inventory inv = createMenu(sec, MAIN_ID);
        fillEmpty(inv, sec.getInt("size", 27));
        addCategoryItems(inv, sec);
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), MAIN_ID);
    }

    public void openEffectsMenu(Player player) {
        ConfigurationSection sec = config.getConfigurationSection("effects-menu");
        if (sec == null) return;
        Inventory inv = createMenu(sec, EFFECTS_ID);
        fillEmpty(inv, sec.getInt("size", 27));
        addShopItems(inv, sec, player);
        addBackButton(inv, sec.getInt("size", 27));
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), EFFECTS_ID);
    }

    public void openFightingMenu(Player player) {
        ConfigurationSection sec = config.getConfigurationSection("fighting-menu");
        if (sec == null) return;
        Inventory inv = createMenu(sec, CRYSTALS_ID);
        fillEmpty(inv, sec.getInt("size", 27));
        addShopItems(inv, sec, player);
        addBackButton(inv, sec.getInt("size", 27));
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), CRYSTALS_ID);
    }

    public void openAmountMenu(Player player, ShopItem item) {
        pendingMultiBuy.put(player.getUniqueId(), item);
        ConfigurationSection sec = config.getConfigurationSection("amount-menu");
        if (sec == null) return;
        int size = sec.getInt("size", 27);
        String title = sec.getString("title", "Select Amount");
        Inventory inv = Bukkit.createInventory(null, size, mm.deserialize(title));
        fillEmpty(inv, size);

        amountSlotMap.clear();
        List<Integer> amounts = sec.getIntegerList("amounts");
        int coins = plugin.getCoinManager().getCoins(player.getUniqueId());
        int startSlot = 10;

        for (int i = 0; i < amounts.size() && startSlot <= 16; i++) {
            int amount = amounts.get(i);
            int totalPrice = item.price * amount;
            boolean canAfford = coins >= totalPrice;

            ItemStack display = new ItemStack(item.material, Math.min(64, amount));
            ItemMeta meta = display.getItemMeta();
            meta.displayName(noItalic(mm.deserialize(
                "<white><bold>" + amount + "x</bold></white> " + item.name)));
            List<Component> lore = new ArrayList<>();
            lore.add(noItalic(mm.deserialize("")));
            lore.add(noItalic(mm.deserialize("<dark_gray>ᴘʀɪᴄᴇ: <green>" + totalPrice + " ᴄᴏɪɴs</green></dark_gray>")));
            lore.add(noItalic(mm.deserialize("")));
            if (canAfford) {
                lore.add(noItalic(mm.deserialize("<green>ʏᴏᴜ ᴄᴀɴ ᴀғғᴏʀᴅ ᴛʜɪs</green>")));
            } else {
                lore.add(noItalic(mm.deserialize("<red>ɴᴏᴛ ᴇɴᴏᴜɢʜ ᴄᴏɪɴs</red> <dark_gray>(" + coins + "/" + totalPrice + ")</dark_gray>")));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(startSlot, display);
            amountSlotMap.put(startSlot, amount);
            startSlot++;
        }

        addBackButton(inv, size);
        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), AMOUNT_ID);
    }

    public String getMenuId(UUID uuid) {
        return openMenus.get(uuid);
    }

    public void removePlayer(UUID uuid) {
        openMenus.remove(uuid);
        pendingMultiBuy.remove(uuid);
    }

    // ====== CLICK HANDLING ======

    public void handleMainClick(Player player, int slot) {
        ConfigurationSection items = config.getConfigurationSection("main-menu.items");
        if (items == null) return;
        for (String key : items.getKeys(false)) {
            int itemSlot = items.getInt(key + ".slot", -1);
            if (itemSlot != slot) continue;
            switch (key) {
                case "effects" -> openEffectsMenu(player);
                case "fighting" -> openFightingMenu(player);
                // coming-soon does nothing
            }
        }
    }

    public void handleCategoryClick(Player player, int slot, String menuId) {
        String menuKey = EFFECTS_ID.equals(menuId) ? "effects-menu" : "fighting-menu";
        ShopItem item = getShopItemAt(menuKey, slot);
        if (item == null) {
            // Check back button
            ConfigurationSection sec = config.getConfigurationSection(menuKey);
            int size = sec != null ? sec.getInt("size", 27) : 27;
            int backSlot = config.getInt("back-button.slot", size - 5);
            if (slot == backSlot) openMainMenu(player);
            return;
        }
        if (item.multiBuy) {
            openAmountMenu(player, item);
        } else {
            purchaseItem(player, item, 1, true);
        }
    }

    // Stores slot -> amount mapping for the amount menu
    private final Map<Integer, Integer> amountSlotMap = new HashMap<>();

    public void handleAmountClick(Player player, int slot) {
        plugin.getLogger().info("[SHOP DEBUG] handleAmountClick slot=" + slot + " player=" + player.getName());
        plugin.getLogger().info("[SHOP DEBUG] pendingMultiBuy=" + (pendingMultiBuy.containsKey(player.getUniqueId())));
        plugin.getLogger().info("[SHOP DEBUG] amountSlotMap=" + amountSlotMap);

        ShopItem item = pendingMultiBuy.get(player.getUniqueId());
        if (item == null) {
            plugin.getLogger().info("[SHOP DEBUG] No pending item, going to main menu");
            openMainMenu(player);
            return;
        }

        // Check back button
        ConfigurationSection sec = config.getConfigurationSection("amount-menu");
        int size = sec != null ? sec.getInt("size", 27) : 27;
        int backSlot = config.getInt("back-button.slot", size - 5);
        if (slot == backSlot) {
            pendingMultiBuy.remove(player.getUniqueId());
            openFightingMenu(player);
            return;
        }

        Integer amount = amountSlotMap.get(slot);
        plugin.getLogger().info("[SHOP DEBUG] amount for slot " + slot + " = " + amount);
        if (amount == null) return;

        if (purchaseItem(player, item, amount, false)) {
            pendingMultiBuy.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    private boolean purchaseItem(Player player, ShopItem item, int amount, boolean closeAfter) {
        int totalPrice = item.price * amount;
        CoinManager cm = plugin.getCoinManager();

        if (cm.getCoins(player.getUniqueId()) < totalPrice) {
            player.sendMessage(plugin.getMessageManager().get("coin-not-enough",
                MessageManager.of("price", String.valueOf(totalPrice))));
            return false;
        }

        if ("effect".equals(item.type) && item.effectType != null) {
            if (player.hasPotionEffect(item.effectType)) {
                player.sendMessage(plugin.getMessageManager().get("shop-active",
                    MessageManager.of("item", item.rawName)));
                return false;
            }
        }

        cm.removeCoins(player.getUniqueId(), totalPrice);
        cm.save();

        if ("effect".equals(item.type) && item.effectType != null) {
            player.addPotionEffect(new PotionEffect(item.effectType, item.duration * 20, item.amplifier, false, true, true));
        } else if ("items".equals(item.type) && item.giveItems != null) {
            for (ItemStack give : item.giveItems) {
                ItemStack clone = give.clone();
                clone.setAmount(give.getAmount() * amount);
                player.getInventory().addItem(clone);
            }
        }

        player.sendMessage(plugin.getMessageManager().get("shop-purchased",
            MessageManager.of("item", (amount > 1 ? amount + "x " : "") + item.rawName, "price", String.valueOf(totalPrice))));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        if (closeAfter) player.closeInventory();
        return true;
    }

    // ====== HELPERS ======

    private Inventory createMenu(ConfigurationSection sec, String id) {
        int size = sec.getInt("size", 27);
        String title = sec.getString("title", "Shop");
        return Bukkit.createInventory(null, size, mm.deserialize(title));
    }

    private void fillEmpty(Inventory inv, int size) {
        Material fillerMat = Material.matchMaterial(config.getString("filler.material", "GRAY_STAINED_GLASS_PANE"));
        if (fillerMat == null) fillerMat = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(noItalic(mm.deserialize(config.getString("filler.name", " "))));
        filler.setItemMeta(fm);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);
    }

    private void addCategoryItems(Inventory inv, ConfigurationSection menuSec) {
        ConfigurationSection items = menuSec.getConfigurationSection("items");
        if (items == null) return;
        for (String key : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(key);
            if (item == null) continue;
            Material mat = Material.matchMaterial(item.getString("material", "STONE"));
            if (mat == null) continue;
            ItemStack display = new ItemStack(mat);
            ItemMeta meta = display.getItemMeta();
            meta.displayName(noItalic(mm.deserialize(item.getString("name", key))));
            List<Component> lore = new ArrayList<>();
            for (String line : item.getStringList("lore")) {
                lore.add(noItalic(mm.deserialize(line)));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(item.getInt("slot", 0), display);
        }
    }

    private void addShopItems(Inventory inv, ConfigurationSection menuSec, Player player) {
        ConfigurationSection items = menuSec.getConfigurationSection("items");
        if (items == null) return;
        int coins = plugin.getCoinManager().getCoins(player.getUniqueId());
        for (String key : items.getKeys(false)) {
            ConfigurationSection sec = items.getConfigurationSection(key);
            if (sec == null) continue;
            Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
            if (mat == null) continue;
            int price = sec.getInt("price", 0);
            ItemStack display = new ItemStack(mat);
            ItemMeta meta = display.getItemMeta();
            meta.displayName(noItalic(mm.deserialize(sec.getString("name", key))));
            List<Component> lore = new ArrayList<>();
            for (String line : sec.getStringList("lore")) {
                lore.add(noItalic(mm.deserialize(line
                    .replace("{price}", String.valueOf(price))
                    .replace("{duration}", sec.getString("duration", "0")))));
            }
            if (coins >= price) {
                lore.add(noItalic(mm.deserialize("")));
                lore.add(noItalic(mm.deserialize("<green>ʏᴏᴜ ᴄᴀɴ ᴀғғᴏʀᴅ ᴛʜɪs</green>")));
            } else {
                lore.add(noItalic(mm.deserialize("")));
                lore.add(noItalic(mm.deserialize("<red>ɴᴏᴛ ᴇɴᴏᴜɢʜ ᴄᴏɪɴs</red> <dark_gray>(" + coins + "/" + price + ")</dark_gray>")));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(sec.getInt("slot", 0), display);
        }
    }

    private void addBackButton(Inventory inv, int size) {
        Material mat = Material.matchMaterial(config.getString("back-button.material", "ARROW"));
        if (mat == null) mat = Material.ARROW;
        int slot = config.getInt("back-button.slot", size - 5);
        ItemStack back = new ItemStack(mat);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(noItalic(mm.deserialize(config.getString("back-button.name", "<gray>← Back</gray>"))));
        back.setItemMeta(meta);
        inv.setItem(slot, back);
    }

    private ShopItem getShopItemAt(String menuKey, int slot) {
        ConfigurationSection items = config.getConfigurationSection(menuKey + ".items");
        if (items == null) return null;
        for (String key : items.getKeys(false)) {
            ConfigurationSection sec = items.getConfigurationSection(key);
            if (sec == null || sec.getInt("slot", -1) != slot) continue;

            ShopItem item = new ShopItem();
            item.key = key;
            item.material = Material.matchMaterial(sec.getString("material", "STONE"));
            item.name = sec.getString("name", key);
            item.rawName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(mm.deserialize(item.name));
            item.price = sec.getInt("price", 0);
            item.type = sec.getString("type", "items");
            item.multiBuy = sec.getBoolean("multi-buy", false);
            if ("effect".equals(item.type)) {
                item.effectType = PotionEffectType.getByName(sec.getString("effect", "STRENGTH"));
                item.amplifier = sec.getInt("amplifier", 0);
                item.duration = sec.getInt("duration", 300);
            } else if ("items".equals(item.type)) {
                item.giveItems = new ArrayList<>();
                for (String entry : sec.getStringList("items")) {
                    String[] parts = entry.split(":");
                    Material m = Material.matchMaterial(parts[0]);
                    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    if (m != null) item.giveItems.add(new ItemStack(m, amount));
                }
            }
            return item;
        }
        return null;
    }

    private Component noItalic(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    public static class ShopItem {
        public String key;
        public Material material;
        public String name;
        public String rawName;
        public int price;
        public String type;
        public PotionEffectType effectType;
        public int amplifier;
        public int duration;
        public List<ItemStack> giveItems;
        public boolean multiBuy;
    }
}
