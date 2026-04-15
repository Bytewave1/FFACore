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
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public class BlockListener implements Listener {

    private final FFACore plugin;

    public BlockListener(FFACore plugin) {
        this.plugin = plugin;
    }

    // === TNT PLACEMENT ===

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlaceLowest(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.TNT) {
            if (plugin.getTntZoneManager().isTntBlocked(event.getBlock().getLocation())) {
                event.setCancelled(true);
            } else {
                event.setCancelled(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlaceMonitor(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.TNT) return;
        Location loc = event.getBlock().getLocation();

        if (plugin.getTntZoneManager().isTntBlocked(loc)) {
            event.setCancelled(true);
            return;
        }

        // Force allow + instant ignite
        event.setCancelled(false);
        event.getBlock().setType(Material.AIR);
        loc.getWorld().spawn(loc.clone().add(0.5, 0.5, 0.5), TNTPrimed.class, tnt -> {
            tnt.setFuseTicks(40);
            tnt.setSource(event.getPlayer());
        });
    }

    // === BLOCK TRACKING ===

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (event.getBlock().getType() == Material.TNT) return; // handled in monitor
        plugin.getArenaManager().trackBlock(event.getBlock().getLocation());
    }

    // === TNT EXPLOSIONS — force allow, override WorldGuard ===

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onExplodeLowest(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onExplodeHighest(EntityExplodeEvent event) {
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockExplodeLowest(BlockExplodeEvent event) {
        event.setCancelled(false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockExplodeMonitor(BlockExplodeEvent event) {
        event.setCancelled(false);
    }

    // === WATER/LAVA TRACKING ===

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
