package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.*;

public class ShulkerListener implements Listener {

    private final FFACore plugin;

    // Track which players have a shulker open and which item slot it was in
    private final Map<UUID, Integer> openShulkerSlot = new HashMap<>();
    private final Set<UUID> openShulkers = new HashSet<>();

    public ShulkerListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isShulkerBox(item.getType())) return;

        event.setCancelled(true);

        // Don't allow in combat
        if (plugin.getCombatManager().isInCombat(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().get("kit-combat"));
            return;
        }

        // Open shulker as inventory
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!(meta.getBlockState() instanceof ShulkerBox shulker)) return;

        Inventory shulkerInv = plugin.getServer().createInventory(null, 27,
            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                "<gradient:#aa55ff:#ff55ff><bold>sʜᴜʟᴋᴇʀ ʙᴏx</bold></gradient>"));

        // Copy contents from shulker to inventory
        ItemStack[] contents = shulker.getInventory().getContents();
        for (int i = 0; i < contents.length && i < 27; i++) {
            if (contents[i] != null) {
                shulkerInv.setItem(i, contents[i].clone());
            }
        }

        int heldSlot = player.getInventory().getHeldItemSlot();
        openShulkerSlot.put(player.getUniqueId(), heldSlot);
        openShulkers.add(player.getUniqueId());
        player.openInventory(shulkerInv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openShulkers.contains(player.getUniqueId())) return;

        // Prevent putting shulker boxes inside shulker boxes
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor != null && isShulkerBox(cursor.getType()) && event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
            return;
        }

        if (event.isShiftClick() && current != null && isShulkerBox(current.getType()) && event.getClickedInventory() != event.getView().getTopInventory()) {
            event.setCancelled(true);
            return;
        }

        // Prevent moving the shulker box item itself
        Integer shulkerSlot = openShulkerSlot.get(player.getUniqueId());
        if (shulkerSlot != null && event.getClickedInventory() != event.getView().getTopInventory()) {
            int clickedSlot = event.getSlot();
            if (clickedSlot == shulkerSlot) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openShulkers.contains(player.getUniqueId())) return;

        // Prevent dragging shulker boxes into shulker
        if (event.getOldCursor() != null && isShulkerBox(event.getOldCursor().getType())) {
            for (int slot : event.getRawSlots()) {
                if (slot < 27) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!openShulkers.remove(player.getUniqueId())) return;

        Integer slot = openShulkerSlot.remove(player.getUniqueId());
        if (slot == null) return;

        // Get the shulker item from the saved slot
        ItemStack shulkerItem = player.getInventory().getItem(slot);
        if (shulkerItem == null || !isShulkerBox(shulkerItem.getType())) return;

        // Create a fresh copy of the item to avoid reference issues
        ItemStack newShulker = shulkerItem.clone();
        BlockStateMeta meta = (BlockStateMeta) newShulker.getItemMeta();
        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();

        // Clear and set contents from the GUI
        shulker.getInventory().clear();
        Inventory topInv = event.getInventory();
        for (int i = 0; i < 27 && i < topInv.getSize(); i++) {
            ItemStack item = topInv.getItem(i);
            shulker.getInventory().setItem(i, item != null ? item.clone() : null);
        }

        // Apply state back
        meta.setBlockState(shulker);
        newShulker.setItemMeta(meta);

        // Replace the item in the player's inventory
        player.getInventory().setItem(slot, newShulker);
        player.updateInventory();
    }

    private boolean isShulkerBox(Material mat) {
        return mat != null && mat.name().contains("SHULKER_BOX");
    }
}
