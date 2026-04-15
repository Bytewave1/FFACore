package dev.warpsmp.ffacore.listener;
import dev.warpsmp.ffacore.util.Scheduler;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public class BlockListener implements Listener {

    private final FFACore plugin;

    public BlockListener(FFACore plugin) {
        this.plugin = plugin;
    }

    // LOWEST: uncancel TNT placement that WorldGuard might have blocked
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlaceLowest(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.TNT) {
            Location loc = event.getBlock().getLocation();
            // Block TNT in no-tnt zones
            if (plugin.getTntZoneManager().isTntBlocked(loc)) {
                event.setCancelled(true);
                return;
            }
            // Force allow TNT everywhere else, override WorldGuard
            event.setCancelled(false);
        }
    }

    // MONITOR: final say — re-enforce TNT zone blocks, and handle instant ignite
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlaceMonitor(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.TNT) {
            Location loc = event.getBlock().getLocation();
            if (plugin.getTntZoneManager().isTntBlocked(loc)) {
                event.setCancelled(true);
                return;
            }
            // Force allow
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Location loc = event.getBlock().getLocation();
        plugin.getArenaManager().trackBlock(loc);

        // Instant TNT ignite
        if (event.getBlock().getType() == Material.TNT) {
            if (plugin.getTntZoneManager().isTntBlocked(loc)) {
                event.setCancelled(true);
                return;
            }
            event.getBlock().setType(Material.AIR);
            loc.getWorld().spawn(loc.clone().add(0.5, 0.5, 0.5), TNTPrimed.class, tnt -> {
                tnt.setFuseTicks(40);
                tnt.setSource(event.getPlayer());
            });
        }
    }

    // Force allow TNT explosions — override WorldGuard
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onExplodeMonitor(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onBucket(PlayerBucketEmptyEvent event) {
        Location loc = event.getBlock().getLocation();
        Scheduler.runAtLocationDelayed(plugin, loc, () -> {
            Block current = loc.getBlock();
            if (current.getType() == Material.WATER || current.getType() == Material.LAVA) {
                plugin.getArenaManager().trackBlock(loc);
            }
        }, 2L);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        Material type = event.getNewState().getType();
        if (type == Material.COBBLESTONE || type == Material.STONE || type == Material.OBSIDIAN) {
            plugin.getArenaManager().trackBlock(event.getBlock().getLocation());
        }
    }
}
