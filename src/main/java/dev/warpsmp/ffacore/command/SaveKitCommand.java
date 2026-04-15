package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SaveKitCommand implements CommandExecutor {

    private final FFACore plugin;

    public SaveKitCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return true;
        }
        plugin.getKitManager().saveKit(player);
        player.sendMessage(plugin.getMessageManager().get("kit-saved",
            MessageManager.of("amount", String.valueOf(plugin.getKitManager().getItemCount()))));
        return true;
    }
}
