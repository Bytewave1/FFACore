package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import dev.warpsmp.ffacore.manager.ShopManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ShopClickListener implements Listener {

    private final FFACore plugin;

    public ShopClickListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title() == null) return;

        String viewTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String shopTitle = PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(plugin.getShopManager().getTitle()));

        if (!viewTitle.equals(shopTitle)) return;

        event.setCancelled(true);

        ShopManager.ShopItem item = plugin.getShopManager().getItem(event.getRawSlot());
        if (item == null) return;

        // Check if effect already active
        if ("effect".equals(item.type) && item.effectType != null && player.hasPotionEffect(item.effectType)) {
            player.sendMessage(plugin.getMessageManager().get("shop-active",
                MessageManager.of("item", PlainTextComponentSerializer.plainText().serialize(
                    MiniMessage.miniMessage().deserialize(item.name)))));
            return;
        }

        // Check coins
        if (plugin.getCoinManager().getCoins(player.getUniqueId()) < item.price) {
            player.sendMessage(plugin.getMessageManager().get("coin-not-enough",
                MessageManager.of("price", String.valueOf(item.price))));
            return;
        }

        // Purchase
        if (plugin.getShopManager().purchase(player, item)) {
            String itemName = PlainTextComponentSerializer.plainText().serialize(
                MiniMessage.miniMessage().deserialize(item.name));
            player.sendMessage(plugin.getMessageManager().get("shop-purchased",
                MessageManager.of("item", itemName, "price", String.valueOf(item.price))));
            player.closeInventory();

            // Play sound
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        }
    }
}
