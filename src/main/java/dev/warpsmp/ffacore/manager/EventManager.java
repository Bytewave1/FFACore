package dev.warpsmp.ffacore.manager;

import dev.warpsmp.ffacore.FFACore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class EventManager {

    private final FFACore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private boolean doubleCoins = false;
    private long doubleCoinsEndTime = 0;
    private BossBar eventBar;

    public EventManager(FFACore plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    public boolean isDoubleCoins() {
        if (doubleCoins && System.currentTimeMillis() > doubleCoinsEndTime) {
            stopDoubleCoins();
            return false;
        }
        return doubleCoins;
    }

    public void startDoubleCoins(int minutes) {
        doubleCoins = true;
        doubleCoinsEndTime = System.currentTimeMillis() + (minutes * 60_000L);
        eventBar = BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(eventBar);
        }
    }

    public void stopDoubleCoins() {
        doubleCoins = false;
        if (eventBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(eventBar);
            }
            eventBar = null;
        }
    }

    public void showBarToPlayer(Player player) {
        if (eventBar != null && doubleCoins) {
            player.showBossBar(eventBar);
        }
    }

    private void startTickTask() {
        Thread thread = new Thread(() -> {
            while (plugin.isEnabled()) {
                try {
                    Thread.sleep(500);
                    if (!doubleCoins || eventBar == null) continue;

                    long remaining = doubleCoinsEndTime - System.currentTimeMillis();
                    if (remaining <= 0) {
                        Bukkit.getGlobalRegionScheduler().run(plugin, t -> stopDoubleCoins());
                        continue;
                    }

                    float progress = Math.max(0f, Math.min(1f, (float) remaining / (float) (doubleCoinsEndTime - (doubleCoinsEndTime - remaining))));
                    // Calculate time display
                    long totalSec = remaining / 1000;
                    long min = totalSec / 60;
                    long sec = totalSec % 60;
                    String timeStr = min > 0 ? min + "m " + sec + "s" : sec + "s";

                    Component title = mm.deserialize(
                        "<white><bold>2</bold></white>" +
                        "<#FAEFF9><bold>x </bold></#FAEFF9>" +
                        "<#F1CFEE><bold>C</bold></#F1CFEE>" +
                        "<#ECBFE8><bold>O</bold></#ECBFE8>" +
                        "<#E7AEE2><bold>I</bold></#E7AEE2>" +
                        "<#E29EDC><bold>N</bold></#E29EDC>" +
                        "<#DE8ED7><bold>S</bold></#DE8ED7>" +
                        " <#D46ECB><bold>f</bold></#D46ECB>" +
                        "<#D362CC><bold>o</bold></#D362CC>" +
                        "<#D156CD><bold>r</bold></#D156CD>" +
                        " <#CF3DCF><bold>(</bold></#CF3DCF>" +
                        "<#CD31D0><bold>" + timeStr + "</bold></#CD31D0>" +
                        "<#C800D4><bold>)</bold></#C800D4>"
                    );

                    // Recalculate proper progress based on initial duration
                    long initialDuration = doubleCoinsEndTime - (doubleCoinsEndTime - remaining);
                    float barProgress = Math.max(0f, (float) remaining / (float) getInitialDuration());

                    eventBar.name(title);
                    eventBar.progress(Math.max(0f, Math.min(1f, barProgress)));
                } catch (Exception ignored) {}
            }
        }, "FFACore-EventTimer");
        thread.setDaemon(true);
        thread.start();
    }

    private long initialDuration = 0;

    public void setInitialDuration(long ms) {
        this.initialDuration = ms;
    }

    public long getInitialDuration() {
        return initialDuration > 0 ? initialDuration : 1;
    }
}
