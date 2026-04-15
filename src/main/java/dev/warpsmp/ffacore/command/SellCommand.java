package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public class SellCommand implements CommandExecutor {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private static final Map<Material, Integer> SELL_PRICES = new LinkedHashMap<>();

    static {
        SELL_PRICES.put(Material.EMERALD, 5);
        SELL_PRICES.put(Material.RAW_GOLD, 2);
        SELL_PRICES.put(Material.DIAMOND, 3);
    }

    public SellCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return true;
        }

        int totalCoins = 0;
        int totalItems = 0;

        for (Map.Entry<Material, Integer> entry : SELL_PRICES.entrySet()) {
            Material mat = entry.getKey();
            int pricePerItem = entry.getValue();

            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() == mat) {
                    totalCoins += item.getAmount() * pricePerItem;
                    totalItems += item.getAmount();
                    player.getInventory().setItem(i, null);
                }
            }
        }

        if (totalItems == 0) {
            player.sendMessage(mm.deserialize(
                plugin.getMessageManager().getRaw("prefix") +
                "<red>ɴᴏᴛʜɪɴɢ ᴛᴏ sᴇʟʟ!</red> <dark_gray>(Emerald, Diamond, Raw Gold)</dark_gray>"));
            return true;
        }

        plugin.getCoinManager().addCoins(player.getUniqueId(), totalCoins);
        plugin.getCoinManager().save();

        int balance = plugin.getCoinManager().getCoins(player.getUniqueId());
        player.sendMessage(mm.deserialize(
            plugin.getMessageManager().getRaw("prefix") +
            "<green>sᴏʟᴅ " + totalItems + " ɪᴛᴇᴍs</green> <dark_gray>→</dark_gray> <green><bold>+" + totalCoins + " ᴄᴏɪɴs</bold></green> <dark_gray>(" + balance + " total)</dark_gray>"));

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        return true;
    }
}
