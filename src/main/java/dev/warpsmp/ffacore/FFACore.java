package dev.warpsmp.ffacore;

import dev.warpsmp.ffacore.command.CoinsCommand;
import dev.warpsmp.ffacore.command.SaveKitCommand;
import dev.warpsmp.ffacore.command.SetCoinsCommand;
import dev.warpsmp.ffacore.command.ShopCommand;
import dev.warpsmp.ffacore.listener.DeathListener;
import dev.warpsmp.ffacore.listener.JoinListener;
import dev.warpsmp.ffacore.listener.ShopClickListener;
import dev.warpsmp.ffacore.manager.CoinManager;
import dev.warpsmp.ffacore.manager.KitManager;
import dev.warpsmp.ffacore.manager.MessageManager;
import dev.warpsmp.ffacore.manager.ShopManager;
import dev.warpsmp.ffacore.placeholder.CoinsExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class FFACore extends JavaPlugin {

    private static FFACore instance;
    private CoinManager coinManager;
    private KitManager kitManager;
    private MessageManager messageManager;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("shop.yml", false);

        messageManager = new MessageManager(this);
        coinManager = new CoinManager(this);
        kitManager = new KitManager(this);
        shopManager = new ShopManager(this);

        getCommand("savekit").setExecutor(new SaveKitCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("coins").setExecutor(new CoinsCommand(this));
        getCommand("setcoins").setExecutor(new SetCoinsCommand(this));

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopClickListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CoinsExpansion(this).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }

        getLogger().info("FFACore enabled!");
    }

    @Override
    public void onDisable() {
        if (coinManager != null) coinManager.save();
        getLogger().info("FFACore disabled!");
    }

    public static FFACore getInstance() { return instance; }
    public CoinManager getCoinManager() { return coinManager; }
    public KitManager getKitManager() { return kitManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public ShopManager getShopManager() { return shopManager; }
}
