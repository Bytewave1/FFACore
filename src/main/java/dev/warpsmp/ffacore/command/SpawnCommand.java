package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final FFACore plugin;

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

        player.teleportAsync(plugin.getSpawnManager().getSpawn());
        player.sendMessage(plugin.getMessageManager().get("spawn-teleported"));
        return true;
    }
}
