package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import dev.warpsmp.ffacore.command.ArenaResetCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public WandListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();
        int modelData = item.getItemMeta().getCustomModelData();

        // Arena wand (9999)
        if (modelData == 9999 && item.getType() == Material.GOLDEN_AXE) {
            event.setCancelled(true);
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

        // AFK zone wand (9998)
        if (modelData == 9998 && item.getType() == Material.BLAZE_ROD) {
            event.setCancelled(true);
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                plugin.getAfkZoneManager().setPos1(player.getUniqueId(), loc);
                player.sendMessage(mm.deserialize(
                    "<gradient:#55ff55:#55ffaa><bold>ᴀғᴋ ᴘᴏs 1</bold></gradient> <gray>set to</gray> <white>" +
                    loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "</white>"));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                plugin.getAfkZoneManager().setPos2(player.getUniqueId(), loc);
                player.sendMessage(mm.deserialize(
                    "<gradient:#55ff55:#55ffaa><bold>ᴀғᴋ ᴘᴏs 2</bold></gradient> <gray>set to</gray> <white>" +
                    loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "</white>"));
            }
        }
    }
}
