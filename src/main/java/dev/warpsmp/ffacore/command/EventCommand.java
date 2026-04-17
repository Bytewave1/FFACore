package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public EventCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().get("prefix")
                .append(mm.deserialize("<gray>Usage: /event <doublecoins|stop> [minutes]</gray>")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "doublecoins" -> {
                if (plugin.getEventManager().isDoubleCoins()) {
                    plugin.getEventManager().stopDoubleCoins();
                }
                int minutes = 10;
                if (args.length >= 2) {
                    try {
                        String input = args[1].toLowerCase().replace("m", "");
                        minutes = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(plugin.getMessageManager().get("prefix")
                            .append(mm.deserialize("<red>Invalid duration.</red>")));
                        return true;
                    }
                }
                plugin.getEventManager().setInitialDuration(minutes * 60_000L);
                plugin.getEventManager().startDoubleCoins(minutes);
                Bukkit.broadcast(mm.deserialize(
                    plugin.getMessageManager().getRaw("prefix") +
                    "<gradient:#C800D4:#FFFFFF><bold>2x ᴄᴏɪɴs ᴇᴠᴇɴᴛ sᴛᴀʀᴛᴇᴅ!</bold></gradient> <gray>(" + minutes + " min)</gray>"));
            }
            case "stop" -> {
                plugin.getEventManager().stopDoubleCoins();
                Bukkit.broadcast(mm.deserialize(
                    plugin.getMessageManager().getRaw("prefix") +
                    "<red><bold>2x ᴄᴏɪɴs ᴇᴠᴇɴᴛ ᴇɴᴅᴇᴅ</bold></red>"));
            }
            default -> {
                sender.sendMessage(plugin.getMessageManager().get("prefix")
                    .append(mm.deserialize("<gray>Usage: /event <doublecoins|stop> [minutes]</gray>")));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            for (String s : List.of("doublecoins", "stop")) {
                if (s.startsWith(args[0].toLowerCase())) subs.add(s);
            }
            return subs;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("doublecoins")) {
            return List.of("5", "10", "15", "30");
        }
        return Collections.emptyList();
    }
}
