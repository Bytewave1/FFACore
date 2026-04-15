package dev.warpsmp.ffacore.listener;
import dev.warpsmp.ffacore.util.Scheduler;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final FFACore plugin;

    public JoinListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean isFirstJoin = !player.hasPlayedBefore();

        // Always teleport to spawn + give kit, with slight delay for chunks to load
        Scheduler.runPlayerDelayed(plugin, player, () -> {
            if (!player.isOnline()) return;

            // Show active bossbars
            plugin.getEventManager().showBarToPlayer(player);
            plugin.getKillstreakManager().showBarToPlayer(player);

            // Teleport to spawn
            if (plugin.getSpawnManager().hasSpawn()) {
                Location spawn = plugin.getSpawnManager().getSpawn();
                player.teleportAsync(spawn).thenAccept(success -> {
                    if (success && isFirstJoin) {
                        // Give random admin kit on first join after teleport
                        Scheduler.runPlayerDelayed(plugin, player, () -> {
                            if (!player.isOnline()) return;
                            plugin.getKitManager().giveRandomAdminKit(player);
                        }, 3L);
                    }
                });
            } else if (isFirstJoin) {
                // No spawn but first join - still give kit
                Scheduler.runPlayerDelayed(plugin, player, () -> {
                    if (!player.isOnline()) return;
                    plugin.getKitManager().giveRandomAdminKit(player);
                }, 3L);
            }
        }, 10L);
    }
}
