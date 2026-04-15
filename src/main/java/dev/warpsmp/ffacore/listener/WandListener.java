package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.command.ArenaResetCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class WandListener implements Listener {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public WandListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!ArenaResetCommand.isWand(event.getItem())) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (event.getClickedBlock() == null) return;
        Location loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            ArenaResetCommand.POS1.put(player.getUniqueId(), loc);
            player.sendMessage(mm.deserialize(
                "<gradient:#ff4444:#ffaa00><bold>ᴘᴏs 1</bold></gradient> <gray>set to</gray> <white>" +
                loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "</white>"));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ArenaResetCommand.POS2.put(player.getUniqueId(), loc);
            player.sendMessage(mm.deserialize(
                "<gradient:#ff4444:#ffaa00><bold>ᴘᴏs 2</bold></gradient> <gray>set to</gray> <white>" +
                loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "</white>"));
        }
    }
}
