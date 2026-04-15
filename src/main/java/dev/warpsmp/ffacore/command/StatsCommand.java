package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public StatsCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        UUID targetUuid;
        String targetName;

        if (args.length >= 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                targetUuid = target.getUniqueId();
                targetName = target.getName();
            } else {
                sender.sendMessage(plugin.getMessageManager().get("player-not-found"));
                return true;
            }
        } else if (sender instanceof Player player) {
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        } else {
            sender.sendMessage(mm.deserialize("<red>Usage: /stats <player></red>"));
            return true;
        }

        StatsManager.PlayerStats stats = plugin.getStatsManager().getStats(targetUuid);
        int rank = plugin.getStatsManager().getRank(targetUuid);
        int currentStreak = plugin.getKillstreakManager().getStreak(targetUuid);
        int coins = plugin.getCoinManager().getCoins(targetUuid);

        // Build the insane stats display
        String kdrColor = stats.getKDR() >= 3.0 ? "#ff4444" : stats.getKDR() >= 1.5 ? "#ffaa00" : stats.getKDR() >= 1.0 ? "#55ff55" : "#aaaaaa";
        String rankColor = rank <= 1 ? "#ffaa00" : rank <= 3 ? "#55ffff" : rank <= 10 ? "#55ff55" : "#aaaaaa";

        // Kill/death bar visualization
        int totalFights = stats.kills + stats.deaths;
        int killBarLen = totalFights > 0 ? (int) Math.round((double) stats.kills / totalFights * 20) : 10;
        int deathBarLen = 20 - killBarLen;
        StringBuilder killBar = new StringBuilder();
        for (int i = 0; i < killBarLen; i++) killBar.append("█");
        StringBuilder deathBar = new StringBuilder();
        for (int i = 0; i < deathBarLen; i++) deathBar.append("█");

        // KDR bar
        double kdr = stats.getKDR();
        int kdrBarLen = (int) Math.min(20, Math.round(kdr * 4));
        StringBuilder kdrBar = new StringBuilder();
        for (int i = 0; i < kdrBarLen; i++) {
            float progress = (float) i / 20f;
            kdrBar.append("<").append(lerpColor(progress)).append(">█</").append(lerpColor(progress)).append(">");
        }
        for (int i = kdrBarLen; i < 20; i++) {
            kdrBar.append("<#333333>█</#333333>");
        }

        sender.sendMessage(mm.deserialize(""));
        sender.sendMessage(mm.deserialize(
            "  <gradient:#00ff88:#00cc66><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(mm.deserialize(""));
        sender.sendMessage(mm.deserialize(
            "    <gradient:#00ff88:#55ffff><bold>ᴘʟᴀʏᴇʀ sᴛᴀᴛs</bold></gradient>  <dark_gray>»</dark_gray>  <white><bold>" + targetName + "</bold></white>"));
        sender.sendMessage(mm.deserialize(""));

        // Rank
        sender.sendMessage(mm.deserialize(
            "    <gray>ʀᴀɴᴋ</gray>        <" + rankColor + "><bold>#" + rank + "</bold></" + rankColor + ">"));

        // Coins
        sender.sendMessage(mm.deserialize(
            "    <gray>ᴄᴏɪɴs</gray>       <green><bold>" + formatNumber(coins) + "</bold></green>"));

        sender.sendMessage(mm.deserialize(""));

        // Kills / Deaths
        sender.sendMessage(mm.deserialize(
            "    <green><bold>" + formatNumber(stats.kills) + "</bold></green> <gray>ᴋɪʟʟs</gray>  <dark_gray>│</dark_gray>  <red><bold>" + formatNumber(stats.deaths) + "</bold></red> <gray>ᴅᴇᴀᴛʜs</gray>"));

        // Kill/Death bar
        sender.sendMessage(mm.deserialize(
            "    <green>" + killBar + "</green><red>" + deathBar + "</red>"));

        sender.sendMessage(mm.deserialize(""));

        // KDR
        sender.sendMessage(mm.deserialize(
            "    <gray>ᴋ/ᴅ ʀᴀᴛɪᴏ</gray>    <" + kdrColor + "><bold>" + String.format("%.2f", stats.getKDR()) + "</bold></" + kdrColor + ">"));
        sender.sendMessage(mm.deserialize("    " + kdrBar));

        sender.sendMessage(mm.deserialize(""));

        // Streaks
        sender.sendMessage(mm.deserialize(
            "    <gray>ᴄᴜʀʀᴇɴᴛ sᴛʀᴇᴀᴋ</gray>  <gradient:#ff4444:#ffaa00><bold>" + currentStreak + "</bold></gradient>"));
        sender.sendMessage(mm.deserialize(
            "    <gray>ʙᴇsᴛ sᴛʀᴇᴀᴋ</gray>     <gradient:#ff4444:#ffaa00><bold>" + stats.highestStreak + "</bold></gradient>"));

        sender.sendMessage(mm.deserialize(""));
        sender.sendMessage(mm.deserialize(
            "  <gradient:#00ff88:#00cc66><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(mm.deserialize(""));

        return true;
    }

    private String formatNumber(int num) {
        if (num >= 1_000_000) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000) return String.format("%.1fk", num / 1_000.0);
        return String.valueOf(num);
    }

    private String lerpColor(float progress) {
        // Green (#00ff88) -> Yellow (#ffff00) -> Red (#ff4444)
        int r, g, b;
        if (progress < 0.5f) {
            float p = progress * 2f;
            r = (int) (0x00 + (0xff - 0x00) * p);
            g = (int) (0xff + (0xff - 0xff) * p);
            b = (int) (0x88 + (0x00 - 0x88) * p);
        } else {
            float p = (progress - 0.5f) * 2f;
            r = 0xff;
            g = (int) (0xff + (0x44 - 0xff) * p);
            b = (int) (0x00 + (0x44 - 0x00) * p);
        }
        return String.format("#%02x%02x%02x", r & 0xff, g & 0xff, b & 0xff);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) names.add(p.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
