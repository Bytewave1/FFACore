package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
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
        sender.sendMessage("§cDieser Befehl ist nicht verfügbar!");
        return true;
    }
}
