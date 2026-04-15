package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final FFACore plugin;

    public JoinListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("kit-on-join", true)) return;
        if (!plugin.getKitManager().hasKit()) return;

        Player player = event.getPlayer();

        // Folia-compatible: use entity scheduler with slight delay
        player.getScheduler().runDelayed(plugin, task -> {
            // Teleport to spawn
            if (plugin.getSpawnManager().hasSpawn()) {
                player.teleportAsync(plugin.getSpawnManager().getSpawn());
            }
            plugin.getKitManager().giveKit(player);
            player.sendMessage(plugin.getMessageManager().get("kit-given"));
        }, null, 5L);
    }
}
