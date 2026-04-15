package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.ArenaManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

import java.util.*;

public class ArenaResetCommand implements CommandExecutor, TabCompleter {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    public static final Map<UUID, Location> POS1 = new HashMap<>();
    public static final Map<UUID, Location> POS2 = new HashMap<>();
    private static final String WAND_NAME = "ffacore_arena_wand";

    public ArenaResetCommand(FFACore plugin) {
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

        switch (args[0].toLowerCase()) {
            case "wand" -> {
                ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
                ItemMeta meta = wand.getItemMeta();
                meta.displayName(mm.deserialize("<gradient:#ff4444:#ffaa00><bold>ᴀʀᴇɴᴀ ᴡᴀɴᴅ</bold></gradient>"));
                meta.lore(List.of(
                    mm.deserialize("<gray>Left click: Set pos 1</gray>"),
                    mm.deserialize("<gray>Right click: Set pos 2</gray>")
                ));
                meta.setCustomModelData(9999);
                wand.setItemMeta(meta);
                player.getInventory().addItem(wand);
                player.sendMessage(plugin.getMessageManager().get("prefix")
                    .append(mm.deserialize("<green>ᴀʀᴇɴᴀ ᴡᴀɴᴅ</green> <dark_gray>received! Left/Right click to select.</dark_gray>")));
            }
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().get("prefix")
                        .append(Component.text("Usage: /arenareset create <name>", NamedTextColor.GRAY)));
                    return true;
                }
                Location p1 = POS1.get(player.getUniqueId());
                Location p2 = POS2.get(player.getUniqueId());
                if (p1 == null || p2 == null) {
                    player.sendMessage(plugin.getMessageManager().get("prefix")
                        .append(mm.deserialize("<red>Select both positions first with the wand!</red>")));
                    return true;
                }
                String name = args[1];
                plugin.getArenaManager().createArena(name, p1, p2);
                ArenaManager.Arena arena = plugin.getArenaManager().getArena(name);
                player.sendMessage(plugin.getMessageManager().get("prefix")
                    .append(mm.deserialize("<green>ᴀʀᴇɴᴀ</green> <white>" + name + "</white> <green>ᴄʀᴇᴀᴛᴇᴅ!</green> <dark_gray>(" + arena.volume() + " blocks)</dark_gray>")));
                POS1.remove(player.getUniqueId());
                POS2.remove(player.getUniqueId());
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().get("prefix")
                        .append(Component.text("Usage: /arenareset delete <name>", NamedTextColor.GRAY)));
                    return true;
                }
                if (plugin.getArenaManager().deleteArena(args[1])) {
                    player.sendMessage(plugin.getMessageManager().get("prefix")
                        .append(mm.deserialize("<red>ᴀʀᴇɴᴀ</red> <white>" + args[1] + "</white> <red>ᴅᴇʟᴇᴛᴇᴅ</red>")));
                } else {
                    player.sendMessage(plugin.getMessageManager().get("prefix")
                        .append(mm.deserialize("<red>Arena not found.</red>")));
                }
            }
            case "list" -> {
                var arenas = plugin.getArenaManager().getArenas();
                if (arenas.isEmpty()) {
                    player.sendMessage(plugin.getMessageManager().get("prefix")
                        .append(mm.deserialize("<gray>No arenas defined.</gray>")));
                } else {
                    player.sendMessage(plugin.getMessageManager().get("prefix")
                        .append(mm.deserialize("<green>ᴀʀᴇɴᴀs:</green>")));
                    for (ArenaManager.Arena a : arenas) {
                        player.sendMessage(mm.deserialize(
                            " <dark_gray>-</dark_gray> <white>" + a.name + "</white> <dark_gray>(" +
                            a.x1 + "," + a.y1 + "," + a.z1 + " → " +
                            a.x2 + "," + a.y2 + "," + a.z2 + " | " + a.volume() + " blocks)</dark_gray>"));
                    }
                }
            }
            case "reset" -> {
                plugin.getArenaManager().resetAllArenas();
                player.sendMessage(plugin.getMessageManager().get("prefix")
                    .append(mm.deserialize("<green>ᴀʟʟ ᴀʀᴇɴᴀs ʀᴇsᴇᴛ!</green>")));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getMessageManager().get("prefix")
            .append(mm.deserialize("<green><bold>ᴀʀᴇɴᴀ ʀᴇsᴇᴛ</bold></green>")));
        player.sendMessage(mm.deserialize(" <green>/arenareset wand</green> <dark_gray>— Get selection wand</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/arenareset create <name></green> <dark_gray>— Create arena from selection</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/arenareset delete <name></green> <dark_gray>— Delete an arena</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/arenareset list</green> <dark_gray>— List all arenas</dark_gray>"));
        player.sendMessage(mm.deserialize(" <green>/arenareset reset</green> <dark_gray>— Force reset all arenas now</dark_gray>"));
    }

    public static boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 9999;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            for (String s : List.of("wand", "create", "delete", "list", "reset")) {
                if (s.startsWith(args[0].toLowerCase())) subs.add(s);
            }
            return subs;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            List<String> names = new ArrayList<>();
            for (ArenaManager.Arena a : plugin.getArenaManager().getArenas()) {
                if (a.name.toLowerCase().startsWith(args[1].toLowerCase())) names.add(a.name);
            }
            return names;
        }
        return Collections.emptyList();
    }
}
