package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

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
        Player player = event.getPlayer();
        if (player.hasPermission("ffacore.arena.bypass")) return;

        // Always protect sandstone blocks everywhere — overrides WorldGuard
        if (ALWAYS_PROTECTED.contains(event.getBlock().getType())) {
            event.setCancelled(true);
            return;
        }

        Location loc = event.getBlock().getLocation();
        if (!plugin.getArenaManager().isInAnyArena(loc)) return;

        if (plugin.getArenaManager().isOriginalBlock(loc)) {
            event.setCancelled(true);
        }
    }

    // Final safety net — runs last, re-cancels even if another plugin uncancelled
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBreakMonitor(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("ffacore.arena.bypass")) return;

        if (ALWAYS_PROTECTED.contains(event.getBlock().getType())) {
            event.setCancelled(true);
            return;
        }

        Location loc = event.getBlock().getLocation();
        if (plugin.getArenaManager().isInAnyArena(loc) && plugin.getArenaManager().isOriginalBlock(loc)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block ->
            plugin.getArenaManager().isInAnyArena(block.getLocation()) &&
            plugin.getArenaManager().isOriginalBlock(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block ->
            plugin.getArenaManager().isInAnyArena(block.getLocation()) &&
            plugin.getArenaManager().isOriginalBlock(block.getLocation()));
    }
}
