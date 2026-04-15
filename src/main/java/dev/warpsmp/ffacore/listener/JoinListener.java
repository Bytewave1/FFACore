package dev.warpsmp.ffacore.listener;

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

        // Always teleport to spawn + give kit, with slight delay for chunks to load
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;

            // Teleport to spawn
            if (plugin.getSpawnManager().hasSpawn()) {
                Location spawn = plugin.getSpawnManager().getSpawn();
                player.teleportAsync(spawn).thenAccept(success -> {
                    if (success) {
                        // Give kit after teleport
                        player.getScheduler().run(plugin, t -> {
                            if (plugin.getConfig().getBoolean("kit-on-join", true) && plugin.getKitManager().hasKit()) {
                                plugin.getKitManager().giveKit(player);
                                player.sendMessage(plugin.getMessageManager().get("kit-given"));
                            }
                        }, null);
                    }
                });
            } else if (plugin.getConfig().getBoolean("kit-on-join", true) && plugin.getKitManager().hasKit()) {
                plugin.getKitManager().giveKit(player);
                player.sendMessage(plugin.getMessageManager().get("kit-given"));
            }
        }, null, 10L);
    }
}
