package com.vprolabs.vunstable.config;


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
    
    // Platform detection
    private final boolean isFolia;
    
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
    private int nukeMaxConcurrent;
    private int nukeQueueSize;
    private String nukeRodName;
    private boolean nukeRodEnchanted;
    
    // Stab config
    private int stabDepth;
    private double stabVelocity;
    private int stabFuseTicks;
    private String stabSpawnMode;
    private boolean stabTeleportEffect;
    private String stabRodName;
    private boolean stabRodEnchanted;
    
    // Performance config
    private boolean asyncSpawning;
    private boolean particlesEnabled;
    private int maxTntPerTick;
    private boolean autoOptimizeSpigot;
    private boolean removeBlockDrops;
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.isFolia = detectFolia();
        instance = this;
        reload();
    }
    
    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public boolean isFolia() {
        return isFolia;
    }
    
    public static ConfigManager getInstance() {
        return instance;
    }
    
    public void reload() {
        try {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
            
            // Load nuke config (platform-specific defaults)
            if (isFolia) {
                this.nukeTotalTnt = config.getInt("nuke.total-tnt", 1000); // Reduced for Folia
            } else {
                this.nukeTotalTnt = config.getInt("nuke.total-tnt", 2000); // Full power for Bukkit
            }
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
            this.nukeMaxConcurrent = config.getInt("nuke.max-concurrent", 1);
            this.nukeQueueSize = config.getInt("nuke.queue-size", 3);
            this.nukeRodName = config.getString("nuke.rod-name", "Nuke");
            this.nukeRodEnchanted = config.getBoolean("nuke.rod-enchanted", true);
            
            // Load stab config
            this.stabDepth = config.getInt("stab.depth", 100);
            this.stabVelocity = config.getDouble("stab.velocity", -10.0);
            this.stabFuseTicks = config.getInt("stab.fuse-ticks", 10);
            this.stabSpawnMode = config.getString("stab.spawn-mode", "INSTANT");
            this.stabTeleportEffect = config.getBoolean("stab.teleport-effect", true);
            this.stabRodName = config.getString("stab.rod-name", "Stab");
            this.stabRodEnchanted = config.getBoolean("stab.rod-enchanted", true);
            
            // Load performance config
            this.asyncSpawning = config.getBoolean("performance.async-spawning", true);
            this.particlesEnabled = config.getBoolean("performance.particles-enabled", false);
            this.maxTntPerTick = config.getInt("performance.max-tnt-per-tick", 200);
            this.autoOptimizeSpigot = config.getBoolean("auto-optimize-spigot", true);
            this.removeBlockDrops = config.getBoolean("remove-block-drops", true);
            
        } catch (Exception e) {
            plugin.getLogger().severe("[vUnstable] Failed to reload config: " + e.getMessage());
        }
    }
    
    // Platform getters
    public boolean isFoliaServer() { return isFolia; }
    
    // Nuke getters
    public int getNukeTotalTnt() { return nukeTotalTnt; }
    
    /**
     * Get platform-optimized spawn rate per tick.
     * Folia: 50 TNT/tick (slower, less region overload)
     * Bukkit: 200 TNT/tick (full speed)
     */
    public int getNukeSpawnRatePerTick() {
        if (isFolia) {
            return config.getInt("nuke.spawn-rate-per-tick", 50);
        }
        return config.getInt("nuke.spawn-rate-per-tick", 200);
    }
    public int getNukeRings() { return nukeRings; }
    public double getNukeMinRadius() { return nukeMinRadius; }
    public double getNukeMaxRadius() { return nukeMaxRadius; }
    public double getNukeSpawnHeight() { return nukeSpawnHeight; }
    public double getNukeVelocityY() { return nukeVelocityY; }
    public boolean isNukeAutoDetonate() { return nukeAutoDetonate; }
    public int getNukeFuseTicks() { return nukeFuseTicks; }
    public int getNukeFinalFuseTicks() { return nukeFinalFuseTicks; }
    public boolean isNukeSyncExplosions() { return nukeSyncExplosions; }
    public int getNukeBaseDelayTicks() { return nukeBaseDelayTicks; }
    public int getNukeMaxConcurrent() { return nukeMaxConcurrent; }
    public int getNukeQueueSize() { return nukeQueueSize; }
    public String getNukeRodName() { return nukeRodName; }
    public boolean isNukeRodEnchanted() { return nukeRodEnchanted; }
    
    // Stab getters
    public int getStabDepth() { return stabDepth; }
    public double getStabVelocity() { return stabVelocity; }
    public int getStabFuseTicks() { return stabFuseTicks; }
    public String getStabSpawnMode() { return stabSpawnMode; }
    public boolean isStabTeleportEffect() { return stabTeleportEffect; }
    public String getStabRodName() { return stabRodName; }
    public boolean isStabRodEnchanted() { return stabRodEnchanted; }
    
    // Performance getters
    public boolean isAsyncSpawning() { return asyncSpawning; }
    public boolean isParticlesEnabled() { return particlesEnabled; }
    public int getMaxTntPerTick() { return maxTntPerTick; }
    public boolean isAutoOptimizeSpigot() { return autoOptimizeSpigot; }
    public boolean shouldRemoveBlockDrops() { return removeBlockDrops; }
    
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
