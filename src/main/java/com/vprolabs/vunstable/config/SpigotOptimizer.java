package com.vprolabs.vunstable.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SpigotOptimizer - Auto-optimizes spigot.yml max-tnt-per-tick for Nuke Rod.
 * 
 * Features multiple fallback paths for panel environments (FoliumPanel, Pterodactyl, etc.)
 * and admin notifications for manual optimization when auto-detection fails.
 */
public class SpigotOptimizer {
    
    public static final int OPTIMAL_VALUE = 5000;
    public static final int TARGET_RATE = 400;
    
    private final JavaPlugin plugin;
    private File spigotYml;
    private final List<String> triedPaths = new ArrayList<>();
    private boolean spigotYmlFound = false;
    private int currentMaxTntPerTick = 100;
    
    /**
     * SpawnParameters - Result of optimization check containing spawn rate and tick count.
     */
    public record SpawnParameters(int ratePerTick, int totalTicks) {}
    
    public SpigotOptimizer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spigotYml = locateSpigotYml();
    }
    
    /**
     * Locates spigot.yml using multiple fallback paths for panel compatibility.
     * Tries paths in order until file.exists() returns true.
     */
    private File locateSpigotYml() {
        triedPaths.clear();
        
        // Try multiple paths in order of likelihood
        File[] candidates = new File[] {
            // Working directory relative
            new File("spigot.yml"),
            // Current directory with explicit path
            new File("./spigot.yml"),
            // User directory (container environments)
            new File(System.getProperty("user.dir"), "spigot.yml"),
            // Bukkit world container parent
            getWorldContainerPath(),
            // Common panel paths
            new File("/server/spigot.yml"),
            new File("/home/container/spigot.yml"),
            new File("/home/minecraft/spigot.yml"),
            new File("/data/spigot.yml"),
            // Parent of working directory
            new File(new File(".").getAbsoluteFile().getParentFile(), "spigot.yml")
        };
        
        for (File candidate : candidates) {
            if (candidate != null) {
                String absolutePath = candidate.getAbsolutePath();
                triedPaths.add(absolutePath);
                
                if (candidate.exists() && candidate.isFile()) {
                    spigotYmlFound = true;
                    plugin.getLogger().info("[vUnstable] Found spigot.yml at: " + absolutePath);
                    return candidate;
                }
            }
        }
        
        // Not found - log warning with all tried paths
        spigotYmlFound = false;
        plugin.getLogger().warning("[vUnstable] Could not locate spigot.yml automatically.");
        plugin.getLogger().warning("[vUnstable] Searched paths:");
        for (String path : triedPaths) {
            plugin.getLogger().warning("[vUnstable]   - " + path);
        }
        
        return null;
    }
    
    /**
     * Gets spigot.yml path from Bukkit's world container.
     */
    private File getWorldContainerPath() {
        try {
            File worldContainer = Bukkit.getWorldContainer();
            if (worldContainer != null) {
                File parent = worldContainer.getParentFile();
                if (parent != null) {
                    return new File(parent, "spigot.yml");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().fine("[vUnstable] Bukkit world container path failed: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Runs the optimization check and optionally auto-optimizes spigot.yml.
     * 
     * @param totalTnt Total TNT to spawn (e.g., 2000)
     * @param targetTicks Target tick count for spawn completion (e.g., 5)
     * @param autoOptimize Whether to attempt writing to spigot.yml
     * @return SpawnParameters with calculated rate and ticks
     */
    public SpawnParameters runOptimizationCheck(int totalTnt, int targetTicks, boolean autoOptimize) {
        plugin.getLogger().info("[vUnstable] Auto-Optimization Check:");
        
        int requiredRate = totalTnt / targetTicks;
        plugin.getLogger().info("[vUnstable] - Required for " + targetTicks + "-tick nuke: " + requiredRate);
        
        currentMaxTntPerTick = readCurrentMaxTntPerTick();
        plugin.getLogger().info("[vUnstable] - Detected max-tnt-per-tick: " + currentMaxTntPerTick);
        
        if (currentMaxTntPerTick >= requiredRate) {
            // Current setting is sufficient
            int actualRate = Math.min(requiredRate, currentMaxTntPerTick);
            int actualTicks = (int) Math.ceil((double) totalTnt / actualRate);
            plugin.getLogger().info("[vUnstable] - SUCCESS: Configured value " + currentMaxTntPerTick + " is sufficient. " +
                "Nuke will spawn " + actualRate + " TNT/tick (" + actualTicks + " ticks total)");
            return new SpawnParameters(actualRate, actualTicks);
        }
        
        // Need to optimize
        if (autoOptimize && spigotYmlFound && spigotYml != null) {
            if (!spigotYml.canWrite()) {
                plugin.getLogger().warning("[vUnstable] Found spigot.yml at: " + spigotYml.getAbsolutePath());
                plugin.getLogger().warning("[vUnstable] File is read-only (panel restrictions).");
                plugin.getLogger().warning("[vUnstable] Please manually set max-tnt-per-tick: " + OPTIMAL_VALUE);
            } else {
                boolean success = attemptOptimization(OPTIMAL_VALUE);
                if (success) {
                    int actualRate = Math.min(requiredRate, OPTIMAL_VALUE);
                    int actualTicks = (int) Math.ceil((double) totalTnt / actualRate);
                    plugin.getLogger().info("[vUnstable] - SUCCESS: Set to " + OPTIMAL_VALUE + ". " +
                        "Nuke will spawn " + actualRate + " TNT/tick (" + actualTicks + " ticks total)");
                    return new SpawnParameters(actualRate, actualTicks);
                } else {
                    plugin.getLogger().warning("[vUnstable] - FAILED: Could not write to spigot.yml. " +
                        "Please manually set max-tnt-per-tick: " + OPTIMAL_VALUE);
                }
            }
        } else if (autoOptimize) {
            plugin.getLogger().warning("[vUnstable] - FAILED: spigot.yml not found. " +
                "Please manually set max-tnt-per-tick: " + OPTIMAL_VALUE + " in spigot.yml");
        }
        
        // Fallback mode - use conservative rate
        int fallbackRate = Math.min(100, currentMaxTntPerTick > 0 ? currentMaxTntPerTick : 100);
        int fallbackTicks = (int) Math.ceil((double) totalTnt / fallbackRate);
        plugin.getLogger().info("[vUnstable] - Fallback mode: Will spawn " + fallbackRate + " TNT/tick (" + fallbackTicks + " ticks total)");
        return new SpawnParameters(fallbackRate, fallbackTicks);
    }
    
    /**
     * Reads current max-tnt-per-tick from spigot.yml.
     * Returns 100 as default if not found or error occurs.
     */
    private int readCurrentMaxTntPerTick() {
        if (!spigotYmlFound || spigotYml == null || !spigotYml.exists()) {
            return 100; // Default Spigot value
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(spigotYml);
            
            // Try world-settings.default.max-tnt-per-tick first
            int value = config.getInt("world-settings.default.max-tnt-per-tick", -1);
            if (value > 0) {
                return value;
            }
            
            // Try world-settings.*.max-tnt-per-tick (get first world)
            if (config.isConfigurationSection("world-settings")) {
                for (String key : config.getConfigurationSection("world-settings").getKeys(false)) {
                    if (!key.equals("default")) {
                        value = config.getInt("world-settings." + key + ".max-tnt-per-tick", -1);
                        if (value > 0) {
                            return value;
                        }
                    }
                }
            }
            
            // Try legacy format (direct max-tnt-per-tick)
            value = config.getInt("max-tnt-per-tick", -1);
            if (value > 0) {
                return value;
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("[vUnstable] Failed to read spigot.yml: " + e.getMessage());
        }
        
        return 100; // Default fallback
    }
    
    /**
     * Attempts to set max-tnt-per-tick in spigot.yml.
     * Returns true if successful, false otherwise.
     */
    private boolean attemptOptimization(int value) {
        if (spigotYml == null) {
            return false;
        }
        
        // Check if file is writable
        if (!spigotYml.canWrite()) {
            plugin.getLogger().warning("[vUnstable] spigot.yml is read-only. Manual edit required.");
            return false;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(spigotYml);
            
            // Set in world-settings.default
            config.set("world-settings.default.max-tnt-per-tick", value);
            
            // Also set in all world-specific settings if they exist
            if (config.isConfigurationSection("world-settings")) {
                for (String key : config.getConfigurationSection("world-settings").getKeys(false)) {
                    if (!key.equals("default")) {
                        config.set("world-settings." + key + ".max-tnt-per-tick", value);
                    }
                }
            }
            
            // Try to save using FileWriter for better error handling
            try (FileWriter writer = new FileWriter(spigotYml)) {
                writer.write(config.saveToString());
            }
            
            return true;
            
        } catch (SecurityException e) {
            plugin.getLogger().warning("[vUnstable] Security exception writing spigot.yml: " + e.getMessage());
        } catch (IOException e) {
            plugin.getLogger().warning("[vUnstable] IO exception writing spigot.yml: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().warning("[vUnstable] Unexpected exception writing spigot.yml: " + e.getMessage());
        }
        
        return false;
    }
    
    // ==================== PUBLIC GETTERS ====================
    
    /**
     * Returns true if the current max-tnt-per-tick meets the target rate.
     */
    public boolean isOptimized() {
        return currentMaxTntPerTick >= TARGET_RATE;
    }
    
    /**
     * Returns true if spigot.yml was found at any path.
     */
    public boolean isSpigotYmlFound() {
        return spigotYmlFound;
    }
    
    /**
     * Returns the absolute path of the found spigot.yml, or null if not found.
     */
    public String getSpigotYmlPath() {
        return spigotYml != null ? spigotYml.getAbsolutePath() : null;
    }
    
    /**
     * Returns the current max-tnt-per-tick value.
     */
    public int getCurrentMaxTntPerTick() {
        return currentMaxTntPerTick;
    }
    
    /**
     * Returns a comma-separated list of all paths that were tried.
     */
    public String getTriedPathsString() {
        return String.join(", ", triedPaths);
    }
    
    /**
     * Returns the list of paths that were tried.
     */
    public List<String> getTriedPaths() {
        return new ArrayList<>(triedPaths);
    }
    
    /**
     * Sets a custom spigot.yml path (for testing purposes).
     */
    public void setSpigotYmlPath(File file) {
        this.spigotYml = file;
        this.spigotYmlFound = (file != null && file.exists());
    }
}
