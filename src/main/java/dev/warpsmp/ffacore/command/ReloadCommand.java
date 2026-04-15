package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ReloadCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        long start = System.currentTimeMillis();

        plugin.reloadConfig();
        plugin.getMessageManager().reload(plugin);
        plugin.getShopManager().reload();

        long time = System.currentTimeMillis() - start;
        sender.sendMessage(mm.deserialize(
            plugin.getMessageManager().getRaw("prefix") +
            "<green>ʀᴇʟᴏᴀᴅᴇᴅ!</green> <dark_gray>(" + time + "ms)</dark_gray>"));
        return true;
    }
}
