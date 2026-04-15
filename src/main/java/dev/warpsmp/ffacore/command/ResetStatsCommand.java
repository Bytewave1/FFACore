package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.MessageManager;
import dev.warpsmp.ffacore.manager.StatsManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResetStatsCommand implements CommandExecutor, TabCompleter {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final String type;

    public ResetStatsCommand(FFACore plugin, String type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageManager().get("prefix")
                .append(mm.deserialize("<gray>Usage: /" + type + "reset <player|*></gray>")));
            return true;
        }

        String target = args[0];

        if (target.equals("*")) {
            // Reset ALL players
            List<StatsManager.PlayerStats> allStats = new ArrayList<>();
            for (var entry : plugin.getStatsManager().getTopKills(Integer.MAX_VALUE)) {
                if (type.equals("kill")) {
                    entry.getValue().kills = 0;
                    entry.getValue().highestStreak = 0;
                } else {
                    entry.getValue().deaths = 0;
                }
            }
            plugin.getStatsManager().save();
            sender.sendMessage(plugin.getMessageManager().get("prefix")
                .append(mm.deserialize("<green>ᴀʟʟ " + type + "s ʀᴇsᴇᴛ!</green>")));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer == null) {
            sender.sendMessage(plugin.getMessageManager().get("player-not-found"));
            return true;
        }

        StatsManager.PlayerStats stats = plugin.getStatsManager().getStats(targetPlayer.getUniqueId());
        if (type.equals("kill")) {
            stats.kills = 0;
            stats.highestStreak = 0;
            plugin.getKillstreakManager().resetStreak(targetPlayer.getUniqueId());
        } else {
            stats.deaths = 0;
        }
        plugin.getStatsManager().save();

        sender.sendMessage(plugin.getMessageManager().get("prefix")
            .append(mm.deserialize("<green>" + type + "s ʀᴇsᴇᴛ ғᴏʀ</green> <white>" + targetPlayer.getName() + "</white>")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            names.add("*");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) names.add(p.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
