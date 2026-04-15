package dev.warpsmp.ffacore;

import dev.warpsmp.ffacore.command.*;
import dev.warpsmp.ffacore.listener.*;
import dev.warpsmp.ffacore.manager.*;
import dev.warpsmp.ffacore.placeholder.CoinsExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class FFACore extends JavaPlugin {

    private static FFACore instance;
    private CoinManager coinManager;
    private KitManager kitManager;
    private MessageManager messageManager;
    private ShopManager shopManager;
    private CombatManager combatManager;
    private SpawnManager spawnManager;

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
        combatManager = new CombatManager(this);
        spawnManager = new SpawnManager(this);

        getCommand("savekit").setExecutor(new SaveKitCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("coins").setExecutor(new CoinsCommand(this));
        getCommand("setcoins").setExecutor(new SetCoinsCommand(this));
        getCommand("adminsetspawn").setExecutor(new AdminSetSpawnCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopClickListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);

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
    public CombatManager getCombatManager() { return combatManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
}
