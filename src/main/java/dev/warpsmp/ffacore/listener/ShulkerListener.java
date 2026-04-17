package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Bukkit;
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
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class ShulkerListener implements Listener {

    private final FFACore plugin;
    // Store the shulker item that was removed from inventory
    private final Map<UUID, ItemStack> storedShulker = new HashMap<>();
    private final Set<UUID> openShulkers = new HashSet<>();

    public ShulkerListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShiftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isShulkerBox(held.getType())) return;

        event.setCancelled(true);

        if (plugin.getCombatManager().isInCombat(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().get("kit-combat"));
            return;
        }

        if (openShulkers.contains(player.getUniqueId())) return;

        // Clone the shulker and remove from hand
        ItemStack shulkerClone = held.clone();
        player.getInventory().setItemInMainHand(null);

        // Read contents
        BlockStateMeta meta = (BlockStateMeta) shulkerClone.getItemMeta();
        ShulkerBox box = (ShulkerBox) meta.getBlockState();

        Inventory gui = Bukkit.createInventory(null, 27,
            MiniMessage.miniMessage().deserialize("<gradient:#aa55ff:#ff55ff><bold>sʜᴜʟᴋᴇʀ ʙᴏx</bold></gradient>"));

        for (int i = 0; i < box.getInventory().getSize() && i < 27; i++) {
            ItemStack item = box.getInventory().getItem(i);
            if (item != null) gui.setItem(i, item.clone());
        }

        storedShulker.put(player.getUniqueId(), shulkerClone);
        openShulkers.add(player.getUniqueId());
        player.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openShulkers.contains(player.getUniqueId())) return;

        boolean isTopInv = event.getRawSlot() < 27;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Block shulker boxes going INTO the shulker GUI
        if (isTopInv && cursor != null && isShulkerBox(cursor.getType())) {
            event.setCancelled(true);
            return;
        }

        // Block shift-click shulker into GUI
        if (event.isShiftClick() && !isTopInv && current != null && isShulkerBox(current.getType())) {
            event.setCancelled(true);
            return;
        }

        // Block hotbar swap with shulker
        if (event.getHotbarButton() >= 0 && isTopInv) {
            ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
            if (hotbar != null && isShulkerBox(hotbar.getType())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openShulkers.contains(player.getUniqueId())) return;

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

        ItemStack shulkerItem = storedShulker.remove(player.getUniqueId());
        if (shulkerItem == null) return;

        // Save GUI contents into shulker
        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        box.getInventory().clear();

        Inventory gui = event.getInventory();
        for (int i = 0; i < 27; i++) {
            ItemStack item = gui.getItem(i);
            box.getInventory().setItem(i, item != null ? item.clone() : null);
        }

        meta.setBlockState(box);
        shulkerItem.setItemMeta(meta);

        // Give shulker back to player
        player.getInventory().addItem(shulkerItem);
        player.updateInventory();
    }

    private boolean isShulkerBox(Material mat) {
        return mat != null && mat.name().contains("SHULKER_BOX");
    }
}
