package com.vprolabs.vunstable.config;

import com.vprolabs.vunstable.util.ErrorHandler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ConfigManager - Singleton for YAML configuration management.
 * 
 * Provides easy access to all config values with defaults.
 */
public class ConfigManager {
    
    private static ConfigManager instance;
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    // Nuke config
    private int nukeTotalTnt;
    private int nukeRings;
    private double nukeMinRadius;
    private double nukeMaxRadius;
    private double nukeSpawnHeight;
    private double nukeVelocityY;
    private int nukeSpawnRatePerTick;
    private boolean nukeAutoDetonate;
    private int nukeFuseTicks;
    private int nukeFinalFuseTicks;
    private boolean nukeSyncExplosions;
    private int nukeBaseDelayTicks;
    
    // Stab config
    private int stabDepth;
    private double stabVelocity;
    private int stabFuseTicks;
    private String stabSpawnMode;
    private boolean stabTeleportEffect;
    
    // Performance config
    private boolean asyncSpawning;
    private boolean particlesEnabled;
    private int maxTntPerTick;
    private boolean autoOptimizeSpigot;
    private boolean removeBlockDrops;
    private boolean errorLogEnabled;
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        reload();
    }
    
    public static ConfigManager getInstance() {
        return instance;
    }
    
    public void reload() {
        try {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
            
            // Load nuke config
            this.nukeTotalTnt = config.getInt("nuke.total-tnt", 2000);
            this.nukeRings = config.getInt("nuke.rings", 10);
            this.nukeMinRadius = config.getDouble("nuke.min-radius", 5.0);
            this.nukeMaxRadius = config.getDouble("nuke.max-radius", 50.0);
            this.nukeSpawnHeight = config.getDouble("nuke.spawn-height", 67.0);
            this.nukeVelocityY = config.getDouble("nuke.velocity-y", -3.0);
            this.nukeSpawnRatePerTick = config.getInt("nuke.spawn-rate-per-tick", 200);
            this.nukeAutoDetonate = config.getBoolean("nuke.auto-detonate-on-ground", true);
            this.nukeFuseTicks = config.getInt("nuke.fuse-ticks", 9999);
            this.nukeFinalFuseTicks = config.getInt("nuke.final-fuse-ticks", 0);
            this.nukeSyncExplosions = config.getBoolean("nuke.sync-explosions", true);
            this.nukeBaseDelayTicks = config.getInt("nuke.base-delay-ticks", 20);
            
            // Load stab config
            this.stabDepth = config.getInt("stab.depth", 100);
            this.stabVelocity = config.getDouble("stab.velocity", -10.0);
            this.stabFuseTicks = config.getInt("stab.fuse-ticks", 10);
            this.stabSpawnMode = config.getString("stab.spawn-mode", "INSTANT");
            this.stabTeleportEffect = config.getBoolean("stab.teleport-effect", true);
            
            // Load performance config
            this.asyncSpawning = config.getBoolean("performance.async-spawning", true);
            this.particlesEnabled = config.getBoolean("performance.particles-enabled", false);
            this.maxTntPerTick = config.getInt("performance.max-tnt-per-tick", 200);
            this.autoOptimizeSpigot = config.getBoolean("auto-optimize-spigot", true);
            this.removeBlockDrops = config.getBoolean("remove-block-drops", true);
            
            // Load error reporting config (simple boolean toggle)
            this.errorLogEnabled = config.getBoolean("Error-Log", true);
            
        } catch (Exception e) {
            plugin.getLogger().severe("[vUnstable] Failed to reload config: " + e.getMessage());
            // Try to use ErrorHandler if available
            if (instance != null && ErrorHandler.getInstance() != null) {
                ErrorHandler.getInstance().handle(e, "ConfigManager.reload()", 
                    "ConfigManager", "reload", 51, null, null, "Config reload failed");
            }
        }
    }
    
    // Nuke getters
    public int getNukeTotalTnt() { return nukeTotalTnt; }
    public int getNukeRings() { return nukeRings; }
    public double getNukeMinRadius() { return nukeMinRadius; }
    public double getNukeMaxRadius() { return nukeMaxRadius; }
    public double getNukeSpawnHeight() { return nukeSpawnHeight; }
    public double getNukeVelocityY() { return nukeVelocityY; }
    public int getNukeSpawnRatePerTick() { return nukeSpawnRatePerTick; }
    public boolean isNukeAutoDetonate() { return nukeAutoDetonate; }
    public int getNukeFuseTicks() { return nukeFuseTicks; }
    public int getNukeFinalFuseTicks() { return nukeFinalFuseTicks; }
    public boolean isNukeSyncExplosions() { return nukeSyncExplosions; }
    public int getNukeBaseDelayTicks() { return nukeBaseDelayTicks; }
    
    // Stab getters
    public int getStabDepth() { return stabDepth; }
    public double getStabVelocity() { return stabVelocity; }
    public int getStabFuseTicks() { return stabFuseTicks; }
    public String getStabSpawnMode() { return stabSpawnMode; }
    public boolean isStabTeleportEffect() { return stabTeleportEffect; }
    
    // Performance getters
    public boolean isAsyncSpawning() { return asyncSpawning; }
    public boolean isParticlesEnabled() { return particlesEnabled; }
    public int getMaxTntPerTick() { return maxTntPerTick; }
    public boolean isAutoOptimizeSpigot() { return autoOptimizeSpigot; }
    public boolean shouldRemoveBlockDrops() { return removeBlockDrops; }
    
    /**
     * Check if error logging to developer Discord is enabled.
     * Users can only toggle this on/off, never change the webhook URL.
     */
    public boolean isErrorLogEnabled() { return errorLogEnabled; }
    
    public boolean isConsoleOnly() {
        return config.getBoolean("messages.console-only", true);
    }
    
    public String getPrefix() {
        return config.getString("messages.prefix", "&7[&cvUnstable&7] ");
    }
    
    /**
     * Get the raw FileConfiguration.
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
