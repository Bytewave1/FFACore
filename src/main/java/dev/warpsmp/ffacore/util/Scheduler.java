package dev.warpsmp.ffacore.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Compatibility layer: works on both Folia and Paper.
 */
public class Scheduler {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runPlayer(Plugin plugin, Player player, Runnable task) {
        if (FOLIA) {
            player.getScheduler().run(plugin, t -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runPlayerDelayed(Plugin plugin, Player player, Runnable task, long delayTicks) {
        if (FOLIA) {
            player.getScheduler().runDelayed(plugin, t -> task.run(), null, Math.max(1L, delayTicks));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runAtLocation(Plugin plugin, Location loc, Runnable task) {
        if (FOLIA) {
            Bukkit.getRegionScheduler().run(plugin, loc, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runAtLocationDelayed(Plugin plugin, Location loc, Runnable task, long delayTicks) {
        if (FOLIA) {
            Bukkit.getRegionScheduler().runDelayed(plugin, loc, t -> task.run(), Math.max(1L, delayTicks));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
