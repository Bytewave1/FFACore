package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class BlockProtectListener implements Listener {

    private final FFACore plugin;

    public BlockProtectListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("ffacore.arena.bypass")) return;

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
