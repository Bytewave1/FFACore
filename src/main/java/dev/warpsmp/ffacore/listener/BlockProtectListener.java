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

    // Force allow ALL block breaking, then re-cancel only sandstone
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBreakForceAllow(BlockBreakEvent event) {
        // Override spawn-protection and any other vanilla blocking
        if (!ALWAYS_PROTECTED.contains(event.getBlock().getType())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        if (ALWAYS_PROTECTED.contains(event.getBlock().getType())) {
            if (!event.getPlayer().hasPermission("ffacore.arena.bypass")) {
                event.setCancelled(true);
                return;
            }
        } else {
            event.setCancelled(false);
        }
        // ALWAYS track broken blocks for arena reset, even for OPs
        plugin.getArenaManager().trackBlock(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBreakMonitor(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission("ffacore.arena.bypass")) return;

        if (ALWAYS_PROTECTED.contains(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }
}
