package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DiscordCommand implements CommandExecutor {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public DiscordCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String url = plugin.getConfig().getString("discord-link", "https://discord.gg/7xrrbvH4nA");

        sender.sendMessage(mm.deserialize(""));
        sender.sendMessage(mm.deserialize(
            "  <gradient:#5865F2:#7289DA><bold>ᴅɪsᴄᴏʀᴅ</bold></gradient>"));
        sender.sendMessage(Component.empty()
            .append(mm.deserialize("  <gray>Join our Discord: </gray>"))
            .append(mm.deserialize("<#5865F2><bold><u>" + url + "</u></bold></#5865F2>")
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<green>Click to join!</green>")))));
        sender.sendMessage(mm.deserialize(""));
        return true;
    }
}
