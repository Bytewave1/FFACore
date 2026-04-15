package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.ShopManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class ShopClickListener implements Listener {

    private final FFACore plugin;

    public ShopClickListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String menuId = plugin.getShopManager().getMenuId(player.getUniqueId());
        if (menuId == null) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();

        switch (menuId) {
            case ShopManager.MAIN_ID -> plugin.getShopManager().handleMainClick(player, slot);
            case ShopManager.EFFECTS_ID -> plugin.getShopManager().handleCategoryClick(player, slot, ShopManager.EFFECTS_ID);
            case ShopManager.CRYSTALS_ID -> plugin.getShopManager().handleCategoryClick(player, slot, ShopManager.CRYSTALS_ID);
            case ShopManager.AMOUNT_ID -> plugin.getShopManager().handleAmountClick(player, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getShopManager().getMenuId(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        plugin.getShopManager().removePlayer(player.getUniqueId());
    }
}
