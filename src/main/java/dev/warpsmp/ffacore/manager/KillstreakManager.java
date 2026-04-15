package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KillstreakManager {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, Integer> streaks = new ConcurrentHashMap<>();
    private BossBar streakBar;
    private UUID topStreakPlayer;
    private int topStreak = 0;

    public KillstreakManager(FFACore plugin) {
        this.plugin = plugin;
        streakBar = BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        startTickTask();
    }

    public void addKill(Player killer) {
        UUID uuid = killer.getUniqueId();
        int streak = streaks.merge(uuid, 1, Integer::sum);

        // Every 3 kills: 2 min Strength
        if (streak % 3 == 0) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 120, 0, false, true, true));
            killer.sendMessage(mm.deserialize(
                plugin.getMessageManager().getRaw("prefix") +
                "<gradient:#ff4444:#ff8888><bold>ᴋɪʟʟsᴛʀᴇᴀᴋ " + streak + "!</bold></gradient> <gray>+2min Strength</gray>"));

            // Broadcast at milestones
            if (streak % 5 == 0) {
                Bukkit.broadcast(mm.deserialize(
                    plugin.getMessageManager().getRaw("prefix") +
                    "<white>" + killer.getName() + "</white> <gray>is on a</gray> " +
                    "<gradient:#ff4444:#ff8888><bold>" + streak + " ᴋɪʟʟsᴛʀᴇᴀᴋ!</bold></gradient>"));
            }
        }

        updateTopStreak();
    }

    public void resetStreak(UUID uuid) {
        streaks.remove(uuid);
        updateTopStreak();
    }

    public int getStreak(UUID uuid) {
        return streaks.getOrDefault(uuid, 0);
    }

    public void showBarToPlayer(Player player) {
        if (topStreak >= 3) {
            player.showBossBar(streakBar);
        }
    }

    private void updateTopStreak() {
        topStreak = 0;
        topStreakPlayer = null;
        for (Map.Entry<UUID, Integer> entry : streaks.entrySet()) {
            if (entry.getValue() > topStreak) {
                topStreak = entry.getValue();
                topStreakPlayer = entry.getKey();
            }
        }

        if (topStreak < 3) {
            // Hide bar from everyone
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(streakBar);
            }
            return;
        }

        // Show bar to everyone
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(streakBar);
        }
    }

    private void startTickTask() {
        Thread thread = new Thread(() -> {
            while (plugin.isEnabled()) {
                try {
                    Thread.sleep(1000);
                    if (topStreak < 3 || topStreakPlayer == null) continue;

                    Player top = Bukkit.getPlayer(topStreakPlayer);
                    String name = top != null ? top.getName() : "???";

                    Component title = mm.deserialize(
                        "<gradient:#ff4444:#ffaa00><bold>ᴋɪʟʟsᴛʀᴇᴀᴋ</bold></gradient> " +
                        "<white>" + name + "</white> " +
                        "<gray>-</gray> " +
                        "<red><bold>" + topStreak + "</bold></red> " +
                        "<gray>ᴋɪʟʟs</gray>");

                    streakBar.name(title);
                    streakBar.progress(Math.min(1f, topStreak / 20f));
                } catch (Exception ignored) {}
            }
        }, "FFACore-StreakBar");
        thread.setDaemon(true);
        thread.start();
    }
}
