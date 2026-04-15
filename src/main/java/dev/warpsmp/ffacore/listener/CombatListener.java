package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatListener implements Listener {

    private final FFACore plugin;

    public CombatListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;
        Player victim = null;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        }
        if (event.getEntity() instanceof Player p) {
            victim = p;
        }

        if (attacker != null && victim != null && !attacker.equals(victim)) {
            plugin.getCombatManager().tag(attacker);
            plugin.getCombatManager().tag(victim);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getCombatManager().isInCombat(player.getUniqueId())) {
            player.setHealth(0);
            plugin.getCombatManager().remove(player.getUniqueId());
        }
    }
}
