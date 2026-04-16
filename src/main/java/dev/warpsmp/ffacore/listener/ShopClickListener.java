package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.ShopManager;
import dev.warpsmp.ffacore.util.Scheduler;
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
        plugin.getLogger().info("[SHOP DEBUG] Click: menuId=" + menuId + " slot=" + slot);

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
        // Delay cleanup by 2 ticks — if we're switching menus, the new menu will
        // re-set the openMenus entry before this cleanup runs
        Scheduler.runPlayerDelayed(plugin, player, () -> {
            // Only remove if player doesn't have a menu open anymore
            if (player.getOpenInventory().getTopInventory().getSize() <= 4) {
                plugin.getShopManager().removePlayer(player.getUniqueId());
            }
        }, 2L);
    }
}
