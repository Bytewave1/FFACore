package dev.warpsmp.ffacore.listener;
import dev.warpsmp.ffacore.util.Scheduler;

import dev.warpsmp.ffacore.FFACore;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlace(BlockPlaceEvent event) {
        Location loc = event.getBlock().getLocation();

        // TNT handling
        if (event.getBlock().getType() == Material.TNT) {
            if (plugin.getTntZoneManager().isTntBlocked(loc)) {
                event.setCancelled(true);
                return;
            }
            // Force allow TNT
            event.setCancelled(false);
            event.getBlock().setType(Material.AIR);
            loc.getWorld().spawn(loc.clone().add(0.5, 0.5, 0.5), TNTPrimed.class, tnt -> {
                tnt.setFuseTicks(40);
                tnt.setSource(event.getPlayer());
            });
            return;
        }

        // Track non-TNT blocks for arena reset
        if (!event.isCancelled()) {
            plugin.getArenaManager().trackBlock(loc);
        }
    }

    // Force TNT explosions
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;
        event.setCancelled(false);

        if (event.blockList().isEmpty()) {
            Location center = tnt.getLocation();
            Scheduler.runAtLocation(plugin, center, () -> {
                center.getWorld().createExplosion(center, 4f, false, true);
            });
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
