package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminSetSpawnCommand implements CommandExecutor {

    private final FFACore plugin;

    public AdminSetSpawnCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return true;
        }
        plugin.getSpawnManager().setSpawn(player.getLocation());
        // Set world spawn + disable spawn radius scatter
        player.getWorld().setSpawnLocation(player.getLocation());
        player.getWorld().setGameRule(org.bukkit.GameRule.SPAWN_RADIUS, 0);
        player.sendMessage(plugin.getMessageManager().get("spawn-set"));
        return true;
    }
}
