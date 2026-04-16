package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Set;

public class BlockProtectListener implements Listener {

    private final FFACore plugin;

    private static final Set<Material> ALWAYS_PROTECTED = Set.of(
        Material.SANDSTONE,
        Material.SANDSTONE_WALL,
        Material.SANDSTONE_STAIRS,
        Material.SANDSTONE_SLAB,
        Material.CHISELED_SANDSTONE,
        Material.CUT_SANDSTONE,
        Material.CUT_SANDSTONE_SLAB,
        Material.SMOOTH_SANDSTONE,
        Material.SMOOTH_SANDSTONE_STAIRS,
        Material.SMOOTH_SANDSTONE_SLAB,
        Material.RED_SANDSTONE,
        Material.RED_SANDSTONE_WALL,
        Material.RED_SANDSTONE_STAIRS,
        Material.RED_SANDSTONE_SLAB,
        Material.CHISELED_RED_SANDSTONE,
        Material.CUT_RED_SANDSTONE,
        Material.CUT_RED_SANDSTONE_SLAB,
        Material.SMOOTH_RED_SANDSTONE,
        Material.SMOOTH_RED_SANDSTONE_STAIRS,
        Material.SMOOTH_RED_SANDSTONE_SLAB
    );

    public BlockProtectListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        // Protect sandstone always
        if (ALWAYS_PROTECTED.contains(event.getBlock().getType())) {
            if (!event.getPlayer().hasPermission("ffacore.arena.bypass")) {
                event.setCancelled(true);
                return;
            }
        }
        // Track broken blocks for arena reset (even if cancelled by WorldGuard, we still track)
        if (!event.isCancelled()) {
            plugin.getArenaManager().trackBlock(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBreakMonitor(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission("ffacore.arena.bypass")) return;

        if (ALWAYS_PROTECTED.contains(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }
}
