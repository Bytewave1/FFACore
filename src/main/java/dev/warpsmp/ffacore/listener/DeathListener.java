package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
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

        // Remove combat tag
        plugin.getCombatManager().remove(victim.getUniqueId());

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // ALWAYS set respawn to spawn
        if (plugin.getSpawnManager().hasSpawn()) {
            Location spawn = plugin.getSpawnManager().getSpawn();
            event.setRespawnLocation(spawn);
        }

        // Give kit after respawn
        if (plugin.getConfig().getBoolean("kit-on-respawn", true) && plugin.getKitManager().hasKit()) {
            player.getScheduler().runDelayed(plugin, task -> {
                if (!player.isOnline()) return;
                plugin.getKitManager().giveKit(player);
                player.showTitle(Title.title(
                    plugin.getMessageManager().get("respawn-title"),
                    plugin.getMessageManager().get("respawn-subtitle"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                ));
            }, null, 3L);
        }
    }
}
