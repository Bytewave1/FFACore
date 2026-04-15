package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeathListener implements Listener {

    private final FFACore plugin;
    public static final Set<UUID> PENDING_RESPAWN = ConcurrentHashMap.newKeySet();

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

        // Reset victim killstreak
        plugin.getKillstreakManager().resetStreak(victim.getUniqueId());

        if (killer != null && !killer.equals(victim)) {
            int amount = plugin.getConfig().getInt("coins-per-kill", 10);
            if (plugin.getEventManager().isDoubleCoins()) {
                amount *= 2;
            }
            plugin.getCoinManager().addCoins(killer.getUniqueId(), amount);
            plugin.getCoinManager().save();

            // Killstreak
            plugin.getKillstreakManager().addKill(killer);

            int total = plugin.getCoinManager().getCoins(killer.getUniqueId());
            String prefix = plugin.getEventManager().isDoubleCoins() ? "<#C800D4><bold>2x</bold></#C800D4> " : "";

            killer.sendMessage(plugin.getMessageManager().get("coin-earn",
                MessageManager.of("amount", prefix + amount, "coins", String.valueOf(total))));

            plugin.getServer().sendMessage(
                plugin.getMessageManager().getRandomKillMessage(killer.getName(), victim.getName()));

            victim.showTitle(Title.title(
                plugin.getMessageManager().get("death-title"),
                plugin.getMessageManager().get("death-subtitle",
                    MessageManager.of("killer", killer.getName())),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))
            ));
        }

        // Mark for respawn handling
        PENDING_RESPAWN.add(victim.getUniqueId());

        // Force instant respawn via Folia scheduler
        victim.getScheduler().runDelayed(plugin, task -> {
            if (!victim.isOnline()) return;
            if (victim.isDead()) {
                victim.spigot().respawn();
            }
        }, null, 1L);

        // Teleport to spawn after respawn (multiple attempts to be safe)
        for (int delay : new int[]{3, 5, 10}) {
            victim.getScheduler().runDelayed(plugin, task -> {
                if (!victim.isOnline() || victim.isDead()) return;
                if (!PENDING_RESPAWN.contains(victim.getUniqueId())) return;

                if (plugin.getSpawnManager().hasSpawn()) {
                    victim.teleportAsync(plugin.getSpawnManager().getSpawn()).thenAccept(success -> {
                        if (success && PENDING_RESPAWN.remove(victim.getUniqueId())) {
                            victim.getScheduler().run(plugin, t -> {
                                if (plugin.getKitManager().hasKit()) {
                                    plugin.getKitManager().giveKit(victim);
                                }
                                victim.showTitle(Title.title(
                                    plugin.getMessageManager().get("respawn-title"),
                                    plugin.getMessageManager().get("respawn-subtitle"),
                                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                                ));
                            }, null);
                        }
                    });
                } else {
                    PENDING_RESPAWN.remove(victim.getUniqueId());
                    if (plugin.getKitManager().hasKit()) {
                        plugin.getKitManager().giveKit(victim);
                    }
                }
            }, null, (long) delay);
        }
    }
}
