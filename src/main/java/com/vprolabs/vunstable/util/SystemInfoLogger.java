package com.vprolabs.vunstable.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * SystemInfoLogger - Generates a system information log file.
 * 
 * v1.1.2: Creates a comprehensive .log file with server and plugin information.
 */
public class SystemInfoLogger {
    
    private final JavaPlugin plugin;
    private final File logFile;
    
    public SystemInfoLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "system-info.log");
    }
    
    /**
     * Generate the system info log file
     */
    public void generate() {
        try {
            // Ensure directory exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile))) {
                writer.println("===============================================");
                writer.println("      vUnstable System Information Log");
                writer.println("===============================================");
                writer.println();
                
                // Plugin Information
                writer.println("[PLUGIN INFORMATION]");
                writer.println("Plugin Name: vUnstable");
                writer.println("Plugin Version: " + plugin.getDescription().getVersion());
                writer.println("Plugin Author: " + plugin.getDescription().getAuthors());
                writer.println("Website: " + plugin.getDescription().getWebsite());
                writer.println();
                
                // Server Information
                writer.println("[SERVER INFORMATION]");
                writer.println("Server Name: " + Bukkit.getServer().getName());
                writer.println("Server Version: " + Bukkit.getVersion());
                writer.println("Bukkit Version: " + Bukkit.getBukkitVersion());
                writer.println("Minecraft Version: " + Bukkit.getMinecraftVersion());
                writer.println("API Version: " + plugin.getDescription().getAPIVersion());
                writer.println();
                
                // Java Information
                writer.println("[JAVA INFORMATION]");
                writer.println("Java Version: " + System.getProperty("java.version"));
                writer.println("Java Vendor: " + System.getProperty("java.vendor"));
                writer.println("Java Home: " + System.getProperty("java.home"));
                writer.println();
                
                // Operating System Information
                writer.println("[OPERATING SYSTEM]");
                writer.println("OS Name: " + System.getProperty("os.name"));
                writer.println("OS Version: " + System.getProperty("os.version"));
                writer.println("OS Architecture: " + System.getProperty("os.arch"));
                
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                writer.println("Available Processors: " + osBean.getAvailableProcessors());
                writer.println("System Load Average: " + osBean.getSystemLoadAverage());
                writer.println();
                
                // Memory Information
                writer.println("[MEMORY INFORMATION]");
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory() / 1024 / 1024;
                long totalMemory = runtime.totalMemory() / 1024 / 1024;
                long freeMemory = runtime.freeMemory() / 1024 / 1024;
                long usedMemory = totalMemory - freeMemory;
                
                writer.println("Max Memory: " + maxMemory + " MB");
                writer.println("Total Memory: " + totalMemory + " MB");
                writer.println("Free Memory: " + freeMemory + " MB");
                writer.println("Used Memory: " + usedMemory + " MB");
                writer.println();
                
                // JVM Arguments
                writer.println("[JVM ARGUMENTS]");
                RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
                List<String> jvmArgs = runtimeMXBean.getInputArguments();
                if (jvmArgs.isEmpty()) {
                    writer.println("No JVM arguments specified");
                } else {
                    for (String arg : jvmArgs) {
                        writer.println("  " + arg);
                    }
                }
                writer.println();
                
                // Uptime
                writer.println("[UPTIME]");
                long uptime = runtimeMXBean.getUptime();
                long uptimeSeconds = uptime / 1000;
                long uptimeMinutes = uptimeSeconds / 60;
                long uptimeHours = uptimeMinutes / 60;
                long uptimeDays = uptimeHours / 24;
                
                writer.println("JVM Uptime: " + uptimeDays + "d " + (uptimeHours % 24) + "h " + 
                    (uptimeMinutes % 60) + "m " + (uptimeSeconds % 60) + "s");
                writer.println();
                
                // Server Status
                writer.println("[SERVER STATUS]");
                writer.println("Online Players: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
                
                double[] tps = Bukkit.getServer().getTPS();
                if (tps.length > 0) {
                    writer.println("TPS (1m): " + String.format("%.2f", tps[0]));
                    if (tps.length > 1) {
                        writer.println("TPS (5m): " + String.format("%.2f", tps[1]));
                    }
                    if (tps.length > 2) {
                        writer.println("TPS (15m): " + String.format("%.2f", tps[2]));
                    }
                }
                writer.println("Whitelist Enabled: " + Bukkit.hasWhitelist());
                writer.println();
                
                // Installed Plugins
                writer.println("[INSTALLED PLUGINS]");
                Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
                writer.println("Total Plugins: " + plugins.length);
                writer.println();
                
                for (Plugin p : plugins) {
                    String status = p.isEnabled() ? "[ENABLED]" : "[DISABLED]";
                    writer.println(status + " " + p.getName() + " v" + p.getDescription().getVersion());
                }
                writer.println();
                
                // Worlds
                writer.println("[WORLDS]");
                Bukkit.getWorlds().forEach(world -> {
                    writer.println("  - " + world.getName() + " (" + world.getEnvironment() + ")");
                    writer.println("      Loaded Chunks: " + world.getLoadedChunks().length);
                    writer.println("      Entities: " + world.getEntities().size());
                    writer.println("      Players: " + world.getPlayers().size());
                });
                writer.println();
                
                // Footer
                writer.println("===============================================");
                writer.println("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.println("Support: discord.gg/SNzUYWbc5Q");
                writer.println("===============================================");
            }
            
            plugin.getLogger().info("[vUnstable] System info log generated: " + logFile.getName());
            
        } catch (IOException e) {
            plugin.getLogger().warning("[vUnstable] Failed to generate system info log: " + e.getMessage());
        }
    }
    
    /**
     * Get the log file
     */
    public File getLogFile() {
        return logFile;
    }
}
