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

        // Only cancel clicks in the shop inventory, not player's own inventory
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            plugin.getLogger().info("[SHOP DEBUG] Click: menuId=" + menuId + " slot=" + slot);

            switch (menuId) {
                case ShopManager.MAIN_ID -> plugin.getShopManager().handleMainClick(player, slot);
                case ShopManager.EFFECTS_ID -> plugin.getShopManager().handleCategoryClick(player, slot, ShopManager.EFFECTS_ID);
                case ShopManager.CRYSTALS_ID -> plugin.getShopManager().handleCategoryClick(player, slot, ShopManager.CRYSTALS_ID);
                case ShopManager.AMOUNT_ID -> plugin.getShopManager().handleAmountClick(player, slot);
            }
        } else {
            // Block shift-clicking items into shop
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getShopManager().getMenuId(player.getUniqueId()) != null) {
            // Only cancel if dragging into top inventory
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Only clean up if not switching to another shop menu
        // Check after 3 ticks if player still has a shop menu open
        final String currentMenu = plugin.getShopManager().getMenuId(player.getUniqueId());
        if (currentMenu == null) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            String newMenu = plugin.getShopManager().getMenuId(player.getUniqueId());
            // If menu is still the same (not switched), player actually closed it
            if (newMenu != null && newMenu.equals(currentMenu)) {
                plugin.getShopManager().removePlayer(player.getUniqueId());
            }
        }, 3L);
    }
}
