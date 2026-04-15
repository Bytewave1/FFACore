package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.TntZoneManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TntCommand implements CommandExecutor, TabCompleter {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public TntCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get("player-only"));
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String prefix = plugin.getMessageManager().getRaw("prefix");

        switch (args[0].toLowerCase()) {
            case "pos1" -> {
                plugin.getTntZoneManager().setPos1(player.getUniqueId(), player.getLocation().getBlock().getLocation());
                player.sendMessage(mm.deserialize(prefix +
                    "<gradient:#ff4444:#ffaa00><bold>ᴛɴᴛ ᴘᴏs 1</bold></gradient> <gray>set to</gray> <white>" +
                    player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ() + "</white>"));
            }
            case "pos2" -> {
                plugin.getTntZoneManager().setPos2(player.getUniqueId(), player.getLocation().getBlock().getLocation());
                player.sendMessage(mm.deserialize(prefix +
                    "<gradient:#ff4444:#ffaa00><bold>ᴛɴᴛ ᴘᴏs 2</bold></gradient> <gray>set to</gray> <white>" +
                    player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ() + "</white>"));
            }
            case "off" -> {
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize(prefix + "<gray>Usage: /tnt off <name></gray>"));
                    return true;
                }
                Location p1 = plugin.getTntZoneManager().getPos1(player.getUniqueId());
                Location p2 = plugin.getTntZoneManager().getPos2(player.getUniqueId());
                if (p1 == null || p2 == null) {
                    player.sendMessage(mm.deserialize(prefix + "<red>Set both positions first! /tnt pos1 & /tnt pos2</red>"));
                    return true;
                }
                String name = args[1];
                plugin.getTntZoneManager().createZone(name, p1, p2);
                player.sendMessage(mm.deserialize(prefix +
                    "<red><bold>ᴛɴᴛ ᴢᴏɴᴇ</bold></red> <white>" + name + "</white> <red>ᴄʀᴇᴀᴛᴇᴅ</red> <dark_gray>(TNT blocked here)</dark_gray>"));
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize(prefix + "<gray>Usage: /tnt delete <name></gray>"));
                    return true;
                }
                if (plugin.getTntZoneManager().deleteZone(args[1])) {
                    player.sendMessage(mm.deserialize(prefix + "<green>ᴛɴᴛ ᴢᴏɴᴇ</green> <white>" + args[1] + "</white> <green>ᴅᴇʟᴇᴛᴇᴅ</green>"));
                } else {
                    player.sendMessage(mm.deserialize(prefix + "<red>Zone not found.</red>"));
                }
            }
            case "list" -> {
                var zones = plugin.getTntZoneManager().getZones();
                if (zones.isEmpty()) {
                    player.sendMessage(mm.deserialize(prefix + "<gray>No TNT zones.</gray>"));
                } else {
                    player.sendMessage(mm.deserialize(prefix + "<red><bold>ᴛɴᴛ ᴢᴏɴᴇs:</bold></red>"));
                    for (TntZoneManager.TntZone z : zones) {
                        player.sendMessage(mm.deserialize(" <dark_gray>-</dark_gray> <white>" + z.name + "</white> <dark_gray>(" +
                            z.x1 + "," + z.y1 + "," + z.z1 + " → " + z.x2 + "," + z.y2 + "," + z.z2 + ")</dark_gray>"));
                    }
                }
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        String prefix = plugin.getMessageManager().getRaw("prefix");
        player.sendMessage(mm.deserialize(prefix + "<red><bold>ᴛɴᴛ ᴢᴏɴᴇs</bold></red>"));
        player.sendMessage(mm.deserialize(" <red>/tnt pos1</red> <dark_gray>— Set position 1</dark_gray>"));
        player.sendMessage(mm.deserialize(" <red>/tnt pos2</red> <dark_gray>— Set position 2</dark_gray>"));
        player.sendMessage(mm.deserialize(" <red>/tnt off <name></red> <dark_gray>— Create no-TNT zone</dark_gray>"));
        player.sendMessage(mm.deserialize(" <red>/tnt delete <name></red> <dark_gray>— Delete zone</dark_gray>"));
        player.sendMessage(mm.deserialize(" <red>/tnt list</red> <dark_gray>— List all zones</dark_gray>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            for (String s : List.of("pos1", "pos2", "off", "delete", "list")) {
                if (s.startsWith(args[0].toLowerCase())) subs.add(s);
            }
            return subs;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            List<String> names = new ArrayList<>();
            for (TntZoneManager.TntZone z : plugin.getTntZoneManager().getZones()) {
                if (z.name.toLowerCase().startsWith(args[1].toLowerCase())) names.add(z.name);
            }
            return names;
        }
        return Collections.emptyList();
    }
}
