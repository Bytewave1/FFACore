package dev.warpsmp.ffacore.listener;

import dev.warpsmp.ffacore.FFACore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatListener implements Listener {

    private final FFACore plugin;

    public CombatListener(FFACore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Only tag if damage actually went through (not cancelled by WorldGuard/spawn protection)
        if (event.isCancelled()) return;
        if (event.getFinalDamage() <= 0) return;

        Player attacker = null;
        Player victim = null;

        if (event.getDamager() instanceof Player p) attacker = p;
        if (event.getEntity() instanceof Player p) victim = p;

        if (attacker != null && victim != null && !attacker.equals(victim)) {
            if (isNearSpawn(attacker) || isNearSpawn(victim)) return;
            plugin.getCombatManager().tag(attacker);
            plugin.getCombatManager().tag(victim);
        }
    }

    // Also prevent all damage at spawn (fall damage, etc after tp)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (isNearSpawn(player)) {
            event.setCancelled(true);
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

    private boolean isNearSpawn(Player player) {
        if (!plugin.getSpawnManager().hasSpawn()) return false;
        Location spawn = plugin.getSpawnManager().getSpawn();
        if (!player.getWorld().equals(spawn.getWorld())) return false;
        double radius = plugin.getConfig().getDouble("spawn-protection-radius", 5.0);
        return player.getLocation().distanceSquared(spawn) <= radius * radius;
    }
}
