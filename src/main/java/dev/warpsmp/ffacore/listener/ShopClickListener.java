package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.ShopManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class ShopClickListener implements Listener {

    private final FFACore plugin;

    public ShopClickListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title() == null) return;

        String menuId = plugin.getShopManager().getMenuId(event.getView().title());
        if (menuId == null) return;

        // Cancel ALL clicks in shop menus — no item moving
        event.setCancelled(true);

        // Ignore clicks in player inventory
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();

        switch (menuId) {
            case ShopManager.MAIN_ID -> plugin.getShopManager().handleMainClick(player, slot);
            case ShopManager.EFFECTS_ID -> plugin.getShopManager().handleCategoryClick(player, slot, ShopManager.EFFECTS_ID);
            case ShopManager.CRYSTALS_ID -> plugin.getShopManager().handleCategoryClick(player, slot, ShopManager.CRYSTALS_ID);
            case ShopManager.AMOUNT_ID -> plugin.getShopManager().handleAmountClick(player, slot);
        }
    }

    // Block dragging items in shop
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().title() == null) return;
        String menuId = plugin.getShopManager().getMenuId(event.getView().title());
        if (menuId != null) {
            event.setCancelled(true);
        }
    }
}
