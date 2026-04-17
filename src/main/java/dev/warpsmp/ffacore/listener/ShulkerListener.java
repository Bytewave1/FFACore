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
import org.bukkit.event.inventory.ClickType;
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
    private final Map<UUID, ItemStack> openShulkerItem = new HashMap<>();
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

        if (plugin.getCombatManager().isInCombat(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().get("kit-combat"));
            return;
        }

        // Read contents from shulker
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();

        Inventory inv = Bukkit.createInventory(null, 27,
            MiniMessage.miniMessage().deserialize("<gradient:#aa55ff:#ff55ff><bold>sʜᴜʟᴋᴇʀ ʙᴏx</bold></gradient>"));

        for (int i = 0; i < shulker.getInventory().getSize() && i < 27; i++) {
            ItemStack content = shulker.getInventory().getItem(i);
            if (content != null) {
                inv.setItem(i, content.clone());
            }
        }

        // Store reference to the original item
        openShulkerItem.put(player.getUniqueId(), item);
        openShulkers.add(player.getUniqueId());
        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openShulkers.contains(player.getUniqueId())) return;

        // Block ALL shulker boxes from entering the shulker inventory
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        boolean isTopInventory = event.getClickedInventory() == event.getView().getTopInventory();

        // Block placing shulker into shulker (cursor has shulker, clicking top inv)
        if (cursor != null && isShulkerBox(cursor.getType()) && isTopInventory) {
            event.setCancelled(true);
            return;
        }

        // Block shift-clicking shulker into shulker
        if (event.isShiftClick() && current != null && isShulkerBox(current.getType()) && !isTopInventory) {
            event.setCancelled(true);
            return;
        }

        // Block number key swapping shulker into top
        if (event.getClick() == ClickType.NUMBER_KEY && isTopInventory) {
            ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
            if (hotbar != null && isShulkerBox(hotbar.getType())) {
                event.setCancelled(true);
                return;
            }
        }

        // Prevent moving the shulker box we're viewing
        if (!isTopInventory && current != null && isShulkerBox(current.getType())) {
            ItemStack originalShulker = openShulkerItem.get(player.getUniqueId());
            if (originalShulker != null && current.isSimilar(originalShulker)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openShulkers.contains(player.getUniqueId())) return;

        // Block dragging shulker boxes into top inventory
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

        ItemStack originalShulker = openShulkerItem.remove(player.getUniqueId());
        if (originalShulker == null) return;

        // Build new contents array from the GUI
        Inventory topInv = event.getInventory();
        ItemStack[] newContents = new ItemStack[27];
        for (int i = 0; i < 27 && i < topInv.getSize(); i++) {
            ItemStack item = topInv.getItem(i);
            newContents[i] = item != null ? item.clone() : null;
        }

        // Write contents to the shulker item
        BlockStateMeta meta = (BlockStateMeta) originalShulker.getItemMeta();
        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
        shulker.getInventory().setContents(newContents);
        meta.setBlockState(shulker);
        originalShulker.setItemMeta(meta);

        // Force update player inventory
        player.updateInventory();
    }

    private boolean isShulkerBox(Material mat) {
        if (mat == null) return false;
        return mat.name().contains("SHULKER_BOX");
    }
}
