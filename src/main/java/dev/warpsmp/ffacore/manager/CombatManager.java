package dev.warpsmp.ffacore.manager;
import dev.warpsmp.ffacore.util.Scheduler;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private final FFACore plugin;
    private final Map<UUID, Long> combatTimers = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();
    private static final long COMBAT_DURATION_MS = 15_000;

    public CombatManager(FFACore plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    public void tag(Player player) {
        combatTimers.put(player.getUniqueId(), System.currentTimeMillis() + COMBAT_DURATION_MS);
    }

    public boolean isInCombat(UUID uuid) {
        Long expiry = combatTimers.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            combatTimers.remove(uuid);
            return false;
        }
        return true;
    }

    public void remove(UUID uuid) {
        combatTimers.remove(uuid);
    }

    private void startTickTask() {
        Thread thread = new Thread(() -> {
            while (plugin.isEnabled()) {
                try {
                    Thread.sleep(200); // update 5x per second
                    for (Map.Entry<UUID, Long> entry : combatTimers.entrySet()) {
                        long remaining = entry.getValue() - System.currentTimeMillis();
                        if (remaining <= 0) {
                            combatTimers.remove(entry.getKey());
                            Player player = plugin.getServer().getPlayer(entry.getKey());
                            if (player != null && player.isOnline()) {
                                Scheduler.runPlayer(plugin, player, () -> {
                                    player.sendActionBar(mm.deserialize(
                                        plugin.getMessageManager().getRaw("combat-end")));
                                });
                            }
                            continue;
                        }
                        Player player = plugin.getServer().getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            double seconds = remaining / 1000.0;
                            String msg = plugin.getMessageManager().getRaw("combat-tag")
                                .replace("{time}", String.format("%.1f", seconds));
                            Component component = mm.deserialize(msg);
                            Scheduler.runPlayer(plugin, player, () -> {
                                player.sendActionBar(component);
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        }, "FFACore-CombatTimer");
        thread.setDaemon(true);
        thread.start();
    }
}
