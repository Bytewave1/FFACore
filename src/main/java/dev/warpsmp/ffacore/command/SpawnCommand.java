package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpawnCommand implements CommandExecutor {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Set<UUID> warmups = new HashSet<>();

    public SpawnCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return true;
        }

        if (plugin.getCombatManager().isInCombat(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().get("spawn-combat"));
            return true;
        }

        if (!plugin.getSpawnManager().hasSpawn()) {
            player.sendMessage(plugin.getMessageManager().get("spawn-not-set"));
            return true;
        }

        if (warmups.contains(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().get("spawn-already-warming"));
            return true;
        }

        warmups.add(player.getUniqueId());
        Location startLoc = player.getLocation().clone();
        int warmup = plugin.getConfig().getInt("spawn-warmup", 3);

        player.sendMessage(plugin.getMessageManager().get("spawn-warmup",
            MessageManager.of("time", String.valueOf(warmup))));

        // Countdown with action bar
        for (int i = warmup; i >= 1; i--) {
            final int sec = i;
            player.getScheduler().runDelayed(plugin, t -> {
                if (!warmups.contains(player.getUniqueId())) return;
                // Check if moved
                if (hasMoved(startLoc, player.getLocation())) {
                    warmups.remove(player.getUniqueId());
                    player.sendMessage(plugin.getMessageManager().get("spawn-cancelled"));
                    return;
                }
                // Check if entered combat
                if (plugin.getCombatManager().isInCombat(player.getUniqueId())) {
                    warmups.remove(player.getUniqueId());
                    player.sendMessage(plugin.getMessageManager().get("spawn-combat"));
                    return;
                }
                String msg = plugin.getMessageManager().getRaw("spawn-countdown")
                    .replace("{time}", String.valueOf(sec));
                player.sendActionBar(mm.deserialize(msg));
            }, null, Math.max(1L, (long) (warmup - sec) * 20L));
        }

        // Teleport after warmup
        player.getScheduler().runDelayed(plugin, t -> {
            warmups.remove(player.getUniqueId());
            if (hasMoved(startLoc, player.getLocation())) {
                player.sendMessage(plugin.getMessageManager().get("spawn-cancelled"));
                return;
            }
            if (plugin.getCombatManager().isInCombat(player.getUniqueId())) {
                player.sendMessage(plugin.getMessageManager().get("spawn-combat"));
                return;
            }
            player.teleportAsync(plugin.getSpawnManager().getSpawn());
            player.sendMessage(plugin.getMessageManager().get("spawn-teleported"));
            // Heal + clear effects but NO kit
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setFireTicks(0);
        }, null, (long) warmup * 20L);

        return true;
    }

    private boolean hasMoved(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
            || from.getBlockY() != to.getBlockY()
            || from.getBlockZ() != to.getBlockZ();
    }
}
