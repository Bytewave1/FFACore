package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KitCommand implements CommandExecutor {

    private final FFACore plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public KitCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return true;
        }

        if (!plugin.getKitManager().hasKit()) {
            sender.sendMessage(plugin.getMessageManager().get("kit-empty"));
            return true;
        }

        // Combat check (bypass with permission)
        if (!player.hasPermission("ffacore.kit.bypass") && plugin.getCombatManager().isInCombat(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().get("kit-combat"));
            return true;
        }

        // Cooldown check (bypass with permission)
        if (!player.hasPermission("ffacore.kit.bypass")) {
            long cooldownMs = plugin.getConfig().getLong("kit-cooldown", 600) * 1000L;
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null) {
                long remaining = (lastUse + cooldownMs) - System.currentTimeMillis();
                if (remaining > 0) {
                    long sec = remaining / 1000;
                    long min = sec / 60;
                    sec = sec % 60;
                    String timeStr = min > 0 ? min + "m " + sec + "s" : sec + "s";
                    player.sendMessage(plugin.getMessageManager().get("kit-cooldown",
                        MessageManager.of("time", timeStr)));
                    return true;
                }
            }
        }

        plugin.getKitManager().giveKit(player);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(plugin.getMessageManager().get("kit-given"));
        return true;
    }
}
