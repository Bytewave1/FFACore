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

        return null;
    }
}
