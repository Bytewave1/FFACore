package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.time.Duration;

public class DeathListener implements Listener {

    private final FFACore plugin;

    public DeathListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Clear drops
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Suppress default death message
        event.deathMessage(null);

        if (killer != null && !killer.equals(victim)) {
            // Award coins
            int amount = plugin.getConfig().getInt("coins-per-kill", 10);
            plugin.getCoinManager().addCoins(killer.getUniqueId(), amount);
            plugin.getCoinManager().save();

            int total = plugin.getCoinManager().getCoins(killer.getUniqueId());

            // Send coin message to killer
            killer.sendMessage(plugin.getMessageManager().get("coin-earn",
                MessageManager.of("amount", String.valueOf(amount), "coins", String.valueOf(total))));

            // Broadcast kill message
            plugin.getServer().sendMessage(
                plugin.getMessageManager().getRandomKillMessage(killer.getName(), victim.getName()));

            // Death title to victim
            victim.showTitle(Title.title(
                plugin.getMessageManager().get("death-title"),
                plugin.getMessageManager().get("death-subtitle",
                    MessageManager.of("killer", killer.getName())),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
            ));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Set respawn location to spawn
        if (plugin.getSpawnManager().hasSpawn()) {
            event.setRespawnLocation(plugin.getSpawnManager().getSpawn());
        }

        // Remove combat tag on death
        plugin.getCombatManager().remove(player.getUniqueId());

        if (!plugin.getConfig().getBoolean("kit-on-respawn", true)) return;

        // Folia-compatible: use entity scheduler
        player.getScheduler().run(plugin, task -> {
            plugin.getKitManager().giveKit(player);
            player.showTitle(Title.title(
                plugin.getMessageManager().get("respawn-title"),
                plugin.getMessageManager().get("respawn-subtitle"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
            ));
        }, null);
    }
}
