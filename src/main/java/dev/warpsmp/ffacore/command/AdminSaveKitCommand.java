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
        plugin.getKitManager().saveDefaultKit(player);
        player.sendMessage(plugin.getMessageManager().get("kit-admin-saved",
            MessageManager.of("amount", String.valueOf(plugin.getKitManager().getDefaultItemCount()))));
        return true;
    }
}
