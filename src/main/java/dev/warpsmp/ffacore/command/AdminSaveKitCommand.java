package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminSaveKitCommand implements CommandExecutor, TabCompleter {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public AdminSaveKitCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize(prefix() + "<gray>Usage: /adminsavekit remove <number|*></gray>"));
                    return true;
                }
                if (args[1].equals("*")) {
                    List<Integer> all = plugin.getKitManager().getAllAdminKitNumbers();
                    for (int num : all) {
                        plugin.getKitManager().deleteAdminKit(num);
                    }
                    player.sendMessage(mm.deserialize(prefix() + "<red>ᴀʟʟ ᴋɪᴛs ᴅᴇʟᴇᴛᴇᴅ!</red> <dark_gray>(" + all.size() + " kits)</dark_gray>"));
                } else {
                    try {
                        int num = Integer.parseInt(args[1]);
                        if (plugin.getKitManager().deleteAdminKit(num)) {
                            player.sendMessage(mm.deserialize(prefix() + "<red>ᴋɪᴛ #" + num + " ᴅᴇʟᴇᴛᴇᴅ</red>"));
                        } else {
                            player.sendMessage(mm.deserialize(prefix() + "<red>Kit #" + num + " not found.</red>"));
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(mm.deserialize(prefix() + "<red>Invalid number.</red>"));
                    }
                }
            }
            case "replace" -> {
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize(prefix() + "<gray>Usage: /adminsavekit replace <number></gray>"));
                    return true;
                }
                try {
                    int num = Integer.parseInt(args[1]);
                    if (!plugin.getKitManager().hasAdminKit(num)) {
                        player.sendMessage(mm.deserialize(prefix() + "<red>Kit #" + num + " not found.</red>"));
                        return true;
                    }
                    plugin.getKitManager().saveAdminKit(num, player);
                    player.sendMessage(mm.deserialize(prefix() + "<green>ᴋɪᴛ #" + num + " ʀᴇᴘʟᴀᴄᴇᴅ!</green>"));
                } catch (NumberFormatException e) {
                    player.sendMessage(mm.deserialize(prefix() + "<red>Invalid number.</red>"));
                }
            }
            case "list" -> {
                List<Integer> kits = plugin.getKitManager().getAllAdminKitNumbers();
                if (kits.isEmpty()) {
                    player.sendMessage(mm.deserialize(prefix() + "<gray>No kits saved.</gray>"));
                } else {
                    player.sendMessage(mm.deserialize(prefix() + "<green>ᴋɪᴛs:</green> <white>" + kits + "</white>"));
                }
            }
            default -> {
                // Try to parse as kit number(s) to save
                for (String arg : args) {
                    try {
                        int kitNumber = Integer.parseInt(arg);
                        if (kitNumber <= 0) {
                            player.sendMessage(mm.deserialize(prefix() + "<red>Kit number must be positive: " + arg + "</red>"));
                            continue;
                        }
                        plugin.getKitManager().saveAdminKit(kitNumber, player);
                        player.sendMessage(mm.deserialize(prefix() + "<green>ᴋɪᴛ #" + kitNumber + " sᴀᴠᴇᴅ!</green>"));
                    } catch (NumberFormatException e) {
                        sendHelp(player);
                        return true;
                    }
                }
            }
        }
        return true;
    }

    private String prefix() {
        return plugin.getMessageManager().getRaw("prefix");
    }

    private void sendHelp(Player player) {
        player.sendMessage(mm.deserialize(prefix() + "<green><bold>ᴀᴅᴍɪɴ ᴋɪᴛ</bold></green>"));
        player.sendMessage(mm.deserialize(" <green>/adminsavekit <number></green> <dark_gray>— Save current inv as kit</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/adminsavekit replace <number></green> <dark_gray>— Replace existing kit</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/adminsavekit remove <number|*></green> <dark_gray>— Delete kit(s)</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/adminsavekit list</green> <dark_gray>— List all kits</dark_gray>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            for (String s : List.of("remove", "replace", "list")) {
                if (s.startsWith(args[0].toLowerCase())) subs.add(s);
            }
            // Also suggest kit numbers
            for (int num : plugin.getKitManager().getAllAdminKitNumbers()) {
                String n = String.valueOf(num);
                if (n.startsWith(args[0])) subs.add(n);
            }
            return subs;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("replace"))) {
            List<String> completions = new ArrayList<>();
            if (args[0].equalsIgnoreCase("remove")) completions.add("*");
            for (int num : plugin.getKitManager().getAllAdminKitNumbers()) {
                String n = String.valueOf(num);
                if (n.startsWith(args[1])) completions.add(n);
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
