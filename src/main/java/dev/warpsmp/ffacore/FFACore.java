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
    private ArenaManager arenaManager;
    private EventManager eventManager;
    private KillstreakManager killstreakManager;
    private StatsManager statsManager;
    private TntZoneManager tntZoneManager;

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
        arenaManager = new ArenaManager(this);
        eventManager = new EventManager(this);
        killstreakManager = new KillstreakManager(this);
        statsManager = new StatsManager(this);
        tntZoneManager = new TntZoneManager(this);

        AdminSaveKitCommand adminKitCmd = new AdminSaveKitCommand(this);
        getCommand("adminsavekit").setExecutor(adminKitCmd);
        getCommand("adminsavekit").setTabCompleter(adminKitCmd);
        getCommand("discord").setExecutor(new DiscordCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("coins").setExecutor(new CoinsCommand(this));
        getCommand("setcoins").setExecutor(new SetCoinsCommand(this));
        getCommand("adminsetspawn").setExecutor(new AdminSetSpawnCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        ArenaResetCommand arenaCmd = new ArenaResetCommand(this);
        getCommand("arenareset").setExecutor(arenaCmd);
        getCommand("arenareset").setTabCompleter(arenaCmd);
        EventCommand eventCmd = new EventCommand(this);
        getCommand("event").setExecutor(eventCmd);
        getCommand("event").setTabCompleter(eventCmd);
        StatsCommand statsCmd = new StatsCommand(this);
        getCommand("stats").setExecutor(statsCmd);
        getCommand("stats").setTabCompleter(statsCmd);
        getCommand("top").setExecutor(new TopCommand(this));
        ResetStatsCommand killResetCmd = new ResetStatsCommand(this, "kill");
        getCommand("killreset").setExecutor(killResetCmd);
        getCommand("killreset").setTabCompleter(killResetCmd);
        ResetStatsCommand deathResetCmd = new ResetStatsCommand(this, "death");
        getCommand("deathreset").setExecutor(deathResetCmd);
        getCommand("deathreset").setTabCompleter(deathResetCmd);
        getCommand("sell").setExecutor(new SellCommand(this));
        TntCommand tntCmd = new TntCommand(this);
        getCommand("tnt").setExecutor(tntCmd);
        getCommand("tnt").setTabCompleter(tntCmd);
        getCommand("ffareload").setExecutor(new ReloadCommand(this));

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopClickListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockProtectListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CoinsExpansion(this).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }

        getLogger().info("FFACore enabled!");
    }

    @Override
    public void onDisable() {
        if (coinManager != null) coinManager.save();
        if (statsManager != null) statsManager.save();
        getLogger().info("FFACore disabled!");
    }

    public static FFACore getInstance() { return instance; }
    public CoinManager getCoinManager() { return coinManager; }
    public KitManager getKitManager() { return kitManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public ShopManager getShopManager() { return shopManager; }
    public CombatManager getCombatManager() { return combatManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public EventManager getEventManager() { return eventManager; }
    public KillstreakManager getKillstreakManager() { return killstreakManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public TntZoneManager getTntZoneManager() { return tntZoneManager; }
}
