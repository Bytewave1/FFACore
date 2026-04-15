package dev.warpsmp.ffacore.command;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.manager.StatsManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TopCommand implements CommandExecutor {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public TopCommand(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<Map.Entry<UUID, StatsManager.PlayerStats>> top = plugin.getStatsManager().getTopKills(10);

        sender.sendMessage(mm.deserialize(""));
        sender.sendMessage(mm.deserialize(
            "  <gradient:#00ff88:#00cc66><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(mm.deserialize(""));
        sender.sendMessage(mm.deserialize(
            "    <gradient:#ffaa00:#ffdd00><bold>ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ</bold></gradient>  <dark_gray>»</dark_gray>  <gray>ᴛᴏᴘ 10 ᴋɪʟʟs</gray>"));
        sender.sendMessage(mm.deserialize(""));

        if (top.isEmpty()) {
            sender.sendMessage(mm.deserialize("    <dark_gray>No stats yet.</dark_gray>"));
        } else {
            for (int i = 0; i < top.size(); i++) {
                StatsManager.PlayerStats s = top.get(i).getValue();
                String rankIcon;
                String nameColor;
                switch (i) {
                    case 0 -> { rankIcon = "<#ffaa00><bold>①</bold></#ffaa00>"; nameColor = "#ffaa00"; }
                    case 1 -> { rankIcon = "<#aaaaaa><bold>②</bold></#aaaaaa>"; nameColor = "#aaaaaa"; }
                    case 2 -> { rankIcon = "<#aa5500><bold>③</bold></#aa5500>"; nameColor = "#aa5500"; }
                    default -> { rankIcon = "<dark_gray><bold>" + (i + 1) + "</bold></dark_gray>"; nameColor = "#888888"; }
                }

                String kdrStr = String.format("%.2f", s.getKDR());
                sender.sendMessage(mm.deserialize(
                    "    " + rankIcon + "  <" + nameColor + ">" + s.name + "</" + nameColor + ">" +
                    "  <dark_gray>│</dark_gray>  <green><bold>" + s.kills + "</bold></green> <gray>ᴋɪʟʟs</gray>" +
                    "  <dark_gray>│</dark_gray>  <gray>" + kdrStr + " ᴋ/ᴅ</gray>"));
            }
        }

        sender.sendMessage(mm.deserialize(""));
        sender.sendMessage(mm.deserialize(
            "  <gradient:#00ff88:#00cc66><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(mm.deserialize(""));
        return true;
    }
}
