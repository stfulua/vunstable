package com.vprolabs.vunstable;

import com.vprolabs.vunstable.command.RodCommand;
import com.vprolabs.vunstable.config.ConfigManager;
import com.vprolabs.vunstable.config.SpigotOptimizer;
import com.vprolabs.vunstable.engine.AsyncSpawnEngine;
import com.vprolabs.vunstable.listener.AdminErrorNotifier;
import com.vprolabs.vunstable.listener.AdminNotifier;
import com.vprolabs.vunstable.listener.EntityListener;
import com.vprolabs.vunstable.listener.PlayerListener;
import com.vprolabs.vunstable.listener.UpdateListener;
import com.vprolabs.vunstable.rod.RodManager;
import com.vprolabs.vunstable.util.ErrorHandler;
import com.vprolabs.vunstable.util.SystemInfoLogger;
import com.vprolabs.vunstable.util.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * vUnstable v1.1.2 - The Ultimate Orbital Strike Cannon
 * 
 * Features:
 * - Async spawning engine
 * - ConfigManager singleton
 * - SpigotOptimizer for auto-tuning max-tnt-per-tick
 * - AdminNotifier for panel environment optimization alerts
 * - UpdateChecker/UpdateListener for automatic update notifications
 * - NMS MethodHandles for performance
 * - Ground detection for auto-detonation
 * - Configurable block drops
 * 
 * @author vProLabs
 * @version 1.1.2
 */
public class vUnstable extends JavaPlugin {
    
    private static vUnstable instance;
    private ConfigManager configManager;
    private AsyncSpawnEngine spawnEngine;
    private RodManager rodManager;
    private SpigotOptimizer optimizer;
    private AdminNotifier adminNotifier;
    private UpdateChecker updateChecker;
    private UpdateListener updateListener;
    private ErrorHandler errorHandler;
    private AdminErrorNotifier adminErrorNotifier;
    private SystemInfoLogger systemInfoLogger;
    private static SpigotOptimizer.SpawnParameters nukeParams;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Log version
        String version = getDescription().getVersion();
        getLogger().info("[vUnstable] Current version: " + version);
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize singletons
        this.configManager = new ConfigManager(this);
        this.spawnEngine = new AsyncSpawnEngine(this);
        this.rodManager = new RodManager(this);
        
        // Initialize ErrorHandler (after ConfigManager)
        this.errorHandler = new ErrorHandler(this);
        
        // Generate system info log
        this.systemInfoLogger = new SystemInfoLogger(this);
        systemInfoLogger.generate();
        
        // Initialize SpigotOptimizer and run optimization check
        this.optimizer = new SpigotOptimizer(this);
        boolean autoOptimize = getConfig().getBoolean("auto-optimize-spigot", true);
        nukeParams = optimizer.runOptimizationCheck(2000, 5, autoOptimize);
        
        // Log spigot.yml detection status
        if (!optimizer.isSpigotYmlFound()) {
            getLogger().warning("[vUnstable] Could not locate spigot.yml automatically.");
            getLogger().warning("[vUnstable] Searched paths: " + optimizer.getTriedPathsString());
            getLogger().warning("[vUnstable] Please ensure spigot.yml exists in server root.");
            getLogger().warning("[vUnstable] Fallback mode active: " + nukeParams.ratePerTick() + " TNT/tick");
        }
        
        getLogger().info("[vUnstable] Spawn plan: " + nukeParams.ratePerTick() + " TNT/tick for " + nukeParams.totalTicks() + " ticks");
        
        // Initialize UpdateChecker and check for updates async
        this.updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates().thenAccept(updateAvailable -> {
            if (updateAvailable) {
                getLogger().info("[vUnstable] Update check complete. Latest: " + updateChecker.getLatestVersion() + 
                    " (Current: " + version + ", Update available: true)");
            } else {
                getLogger().info("[vUnstable] Update check complete. You are running the latest version.");
            }
        }).exceptionally(ex -> {
            getLogger().warning("[vUnstable] Update check failed: " + ex.getMessage());
            return null;
        });
        
        // Register listeners
        EntityListener entityListener = new EntityListener(this);
        getServer().getPluginManager().registerEvents(entityListener, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Link EntityListener to AsyncSpawnEngine for world tracking
        spawnEngine.setEntityListener(entityListener);
        
        // Register admin notifier for optimization alerts
        this.adminNotifier = new AdminNotifier(this, optimizer);
        getServer().getPluginManager().registerEvents(adminNotifier, this);
        
        // Register update listener for update notifications
        this.updateListener = new UpdateListener(this, updateChecker);
        getServer().getPluginManager().registerEvents(updateListener, this);
        
        // Register admin error notifier for error notifications
        this.adminErrorNotifier = new AdminErrorNotifier(this);
        getServer().getPluginManager().registerEvents(adminErrorNotifier, this);
        
        // Register commands
        RodCommand rodCommand = new RodCommand(this);
        
        if (getCommand("vunstable") != null) {
            getCommand("vunstable").setExecutor(rodCommand);
            getCommand("vunstable").setTabCompleter(rodCommand);
            getLogger().info("[vUnstable] Command 'vunstable' registered successfully");
        } else {
            getLogger().severe("[vUnstable] FAILED to register command 'vunstable' - check plugin.yml!");
        }
        
        if (getCommand("rod") != null) {
            getCommand("rod").setExecutor(rodCommand);
            getCommand("rod").setTabCompleter(rodCommand);
            getLogger().info("[vUnstable] Command 'rod' registered successfully");
        } else {
            getLogger().warning("[vUnstable] Command 'rod' not registered (alias may be handled by 'vunstable')");
        }
        
        getLogger().info("vUnstable v1.1.2 enabled");
    }
    
    @Override
    public void onDisable() {
        if (spawnEngine != null) {
            spawnEngine.shutdown();
        }
        getLogger().info("vUnstable v1.1.2 disabled");
    }
    
    public static vUnstable getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public AsyncSpawnEngine getSpawnEngine() {
        return spawnEngine;
    }
    
    public RodManager getRodManager() {
        return rodManager;
    }
    
    public SpigotOptimizer getOptimizer() {
        return optimizer;
    }
    
    public AdminNotifier getAdminNotifier() {
        return adminNotifier;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
    
    public UpdateListener getUpdateListener() {
        return updateListener;
    }
    
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    /**
     * Gets the optimized spawn parameters for Nuke Rod.
     * @return SpawnParameters containing ratePerTick and totalTicks
     */
    public static SpigotOptimizer.SpawnParameters getNukeParams() {
        return nukeParams;
    }
}
