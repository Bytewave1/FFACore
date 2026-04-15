package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminSaveKitCommand implements CommandExecutor {

    private final FFACore plugin;

    public AdminSaveKitCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return true;
        }

        // Parse all arguments as kit numbers
        if (args.length == 0) {
            player.sendMessage("§cUsage: /adminsavekit <number> [number2] [number3] ...");
            return true;
        }

        for (String arg : args) {
            try {
                int kitNumber = Integer.parseInt(arg);
                if (kitNumber <= 0) {
                    player.sendMessage("§cKit number must be positive: " + arg);
                    continue;
                }
                plugin.getKitManager().saveAdminKit(kitNumber, player);
                player.sendMessage("§aAdmin kit #" + kitNumber + " saved!");
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid kit number: " + arg);
            }
        }
        return true;
    }
}
