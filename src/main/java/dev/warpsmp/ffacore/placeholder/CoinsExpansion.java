package dev.warpsmp.ffacore.placeholder;

import dev.warpsmp.ffacore.FFACore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CoinsExpansion extends PlaceholderExpansion {

    private final FFACore plugin;

    public CoinsExpansion(FFACore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ffacore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Bytewave1";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "0";

        if (params.equalsIgnoreCase("coins")) {
            return String.valueOf(plugin.getCoinManager().getCoins(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("coins_formatted")) {
            int coins = plugin.getCoinManager().getCoins(player.getUniqueId());
            if (coins >= 1000) return String.format("%.1fk", coins / 1000.0);
            return String.valueOf(coins);
        }

        if (params.equalsIgnoreCase("kills")) {
            return String.valueOf(plugin.getStatsManager().getStats(player.getUniqueId()).kills);
        }
        if (params.equalsIgnoreCase("deaths")) {
            return String.valueOf(plugin.getStatsManager().getStats(player.getUniqueId()).deaths);
        }
        if (params.equalsIgnoreCase("kdr")) {
            return String.format("%.2f", plugin.getStatsManager().getStats(player.getUniqueId()).getKDR());
        }
        if (params.equalsIgnoreCase("streak")) {
            return String.valueOf(plugin.getKillstreakManager().getStreak(player.getUniqueId()));
        }
        if (params.equalsIgnoreCase("best_streak")) {
            return String.valueOf(plugin.getStatsManager().getStats(player.getUniqueId()).highestStreak);
        }
        if (params.equalsIgnoreCase("rank")) {
            return String.valueOf(plugin.getStatsManager().getRank(player.getUniqueId()));
        }

        return null;
    }
}
