package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public class BlockListener implements Listener {

    private final FFACore plugin;

    public BlockListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        scheduleRemoval(event.getBlock().getLocation());
    }

    @EventHandler
    public void onBucket(PlayerBucketEmptyEvent event) {
        Location loc = event.getBlock().getLocation();
        Bukkit.getRegionScheduler().runDelayed(plugin, loc, task -> {
            Block current = loc.getBlock();
            if (current.getType() == Material.WATER || current.getType() == Material.LAVA) {
                scheduleRemoval(loc);
            }
        }, 2L);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        Material type = event.getNewState().getType();
        if (type == Material.COBBLESTONE || type == Material.STONE || type == Material.OBSIDIAN) {
            scheduleRemoval(event.getBlock().getLocation());
        }
    }

    private void scheduleRemoval(Location loc) {
        long removeDelayTicks = plugin.getConfig().getLong("block-remove-delay", 60) * 20L;

        Bukkit.getRegionScheduler().runDelayed(plugin, loc, task -> {
            Block current = loc.getBlock();
            if (current.getType() != Material.AIR) {
                current.setType(Material.AIR);
            }
        }, removeDelayTicks);
    }
}
