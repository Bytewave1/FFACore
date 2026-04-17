package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.AfkZoneManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AfkZoneCommand implements CommandExecutor, TabCompleter {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public AfkZoneCommand(FFACore plugin) {
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
            case "wand" -> {
                ItemStack wand = new ItemStack(Material.BLAZE_ROD);
                ItemMeta meta = wand.getItemMeta();
                meta.displayName(mm.deserialize("<gradient:#55ff55:#55ffaa><bold>ᴀғᴋ ᴢᴏɴᴇ ᴡᴀɴᴅ</bold></gradient>"));
                meta.lore(List.of(
                    mm.deserialize("<gray>Left click: Set pos 1</gray>"),
                    mm.deserialize("<gray>Right click: Set pos 2</gray>")
                ));
                meta.setCustomModelData(9998);
                wand.setItemMeta(meta);
                player.getInventory().addItem(wand);
                player.sendMessage(mm.deserialize(prefix + "<green>ᴀғᴋ ᴢᴏɴᴇ ᴡᴀɴᴅ</green> <dark_gray>received!</dark_gray>"));
            }
            case "pos1" -> {
                plugin.getAfkZoneManager().setPos1(player.getUniqueId(), player.getLocation().getBlock().getLocation());
                player.sendMessage(mm.deserialize(prefix +
                    "<gradient:#55ff55:#55ffaa><bold>ᴀғᴋ ᴘᴏs 1</bold></gradient> <gray>set to</gray> <white>" +
                    player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ() + "</white>"));
            }
            case "pos2" -> {
                plugin.getAfkZoneManager().setPos2(player.getUniqueId(), player.getLocation().getBlock().getLocation());
                player.sendMessage(mm.deserialize(prefix +
                    "<gradient:#55ff55:#55ffaa><bold>ᴀғᴋ ᴘᴏs 2</bold></gradient> <gray>set to</gray> <white>" +
                    player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ() + "</white>"));
            }
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize(prefix + "<gray>Usage: /afkzone create <name></gray>"));
                    return true;
                }
                Location p1 = plugin.getAfkZoneManager().getPos1(player.getUniqueId());
                Location p2 = plugin.getAfkZoneManager().getPos2(player.getUniqueId());
                if (p1 == null || p2 == null) {
                    player.sendMessage(mm.deserialize(prefix + "<red>Set both positions first!</red>"));
                    return true;
                }
                plugin.getAfkZoneManager().createZone(args[1], p1, p2);
                player.sendMessage(mm.deserialize(prefix +
                    "<green>ᴀғᴋ ᴢᴏɴᴇ</green> <white>" + args[1] + "</white> <green>ᴄʀᴇᴀᴛᴇᴅ!</green>"));
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(mm.deserialize(prefix + "<gray>Usage: /afkzone delete <name></gray>"));
                    return true;
                }
                if (plugin.getAfkZoneManager().deleteZone(args[1])) {
                    player.sendMessage(mm.deserialize(prefix +
                        "<red>ᴀғᴋ ᴢᴏɴᴇ</red> <white>" + args[1] + "</white> <red>ᴅᴇʟᴇᴛᴇᴅ</red>"));
                } else {
                    player.sendMessage(mm.deserialize(prefix + "<red>Zone not found.</red>"));
                }
            }
            case "list" -> {
                var zones = plugin.getAfkZoneManager().getZones();
                if (zones.isEmpty()) {
                    player.sendMessage(mm.deserialize(prefix + "<gray>No AFK zones.</gray>"));
                } else {
                    player.sendMessage(mm.deserialize(prefix + "<green><bold>ᴀғᴋ ᴢᴏɴᴇs:</bold></green>"));
                    for (AfkZoneManager.AfkZone z : zones) {
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
        player.sendMessage(mm.deserialize(prefix + "<green><bold>ᴀғᴋ ᴢᴏɴᴇs</bold></green>"));
        player.sendMessage(mm.deserialize(" <green>/afkzone wand</green> <dark_gray>— Get selection wand</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/afkzone pos1</green> <dark_gray>— Set position 1</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/afkzone pos2</green> <dark_gray>— Set position 2</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/afkzone create <name></green> <dark_gray>— Create AFK zone</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/afkzone delete <name></green> <dark_gray>— Delete zone</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/afkzone list</green> <dark_gray>— List all zones</dark_gray>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            for (String s : List.of("wand", "pos1", "pos2", "create", "delete", "list")) {
                if (s.startsWith(args[0].toLowerCase())) subs.add(s);
            }
            return subs;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            List<String> names = new ArrayList<>();
            for (AfkZoneManager.AfkZone z : plugin.getAfkZoneManager().getZones()) {
                if (z.name.toLowerCase().startsWith(args[1].toLowerCase())) names.add(z.name);
            }
            return names;
        }
        return Collections.emptyList();
    }
}
