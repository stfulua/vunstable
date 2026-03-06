package com.vprolabs.vunstable.util;

import com.vprolabs.vunstable.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * ErrorHandler - Comprehensive error handling and reporting system.
 * 
 * Features:
 * - Detailed error log files with server specs
 * - Error classification (CRITICAL, WARNING, INFO)
 * - Discord webhook integration
 * - Rate limiting to prevent spam
 * - File rotation for large logs
 */
public class ErrorHandler {
    
    private static ErrorHandler instance;
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final DiscordWebhook discordWebhook;
    
    private final File errorsDir;
    private final File archiveDir;
    private final BlockingQueue<ErrorReport> errorQueue;
    
    private long lastDiscordSend = 0;
    private static final long DISCORD_RATE_LIMIT_MS = 300000; // 5 minutes
    private int errorCount = 0;
    private long sessionStartTime;
    
    // Error severity levels
    public enum Severity {
        CRITICAL("CRITICAL", 0xFF0000),    // Red - Immediate Discord + Console
        WARNING("WARNING", 0xFFA500),      // Orange - Discord + File
        INFO("INFO", 0x3498DB);            // Blue - File only
        
        private final String name;
        private final int color;
        
        Severity(String name, int color) {
            this.name = name;
            this.color = color;
        }
        
        public String getName() { return name; }
        public int getColor() { return color; }
    }
    
    /**
     * Represents a single error report
     */
    public static class ErrorReport {
        public final Throwable error;
        public final String context;
        public final String className;
        public final String methodName;
        public final int lineNumber;
        public final Player player;
        public final Location location;
        public final String additionalInfo;
        public final Severity severity;
        public final long timestamp;
        
        public ErrorReport(Throwable error, String context, String className, String methodName, 
                          int lineNumber, Player player, Location location, String additionalInfo, 
                          Severity severity) {
            this.error = error;
            this.context = context;
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
            this.player = player;
            this.location = location;
            this.additionalInfo = additionalInfo;
            this.severity = severity;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public ErrorHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ConfigManager.getInstance();
        this.discordWebhook = new DiscordWebhook(plugin);
        this.errorsDir = new File(plugin.getDataFolder(), "errors");
        this.archiveDir = new File(errorsDir, "archive");
        this.errorQueue = new LinkedBlockingQueue<>();
        this.sessionStartTime = System.currentTimeMillis();
        
        initializeDirectories();
        instance = this;
        
        // Start error processing thread
        startErrorProcessor();
    }
    
    public static ErrorHandler getInstance() {
        return instance;
    }
    
    private void initializeDirectories() {
        if (!errorsDir.exists()) {
            errorsDir.mkdirs();
        }
        if (!archiveDir.exists()) {
            archiveDir.mkdirs();
        }
    }
    
    /**
     * Main entry point for error handling
     */
    public void handle(Throwable error, String context, Player player, Location location, String additionalInfo) {
        handle(error, context, null, null, -1, player, location, additionalInfo);
    }
    
    /**
     * Handle error with full context
     */
    public void handle(Throwable error, String context, String className, String methodName, 
                      int lineNumber, Player player, Location location, String additionalInfo) {
        
        if (error == null) return;
        
        // Determine severity
        Severity severity = classifyError(error);
        
        // Create error report
        ErrorReport report = new ErrorReport(error, context, className, methodName, 
            lineNumber, player, location, additionalInfo, severity);
        
        // Increment counter
        errorCount++;
        
        // Log to console immediately
        logToConsole(report);
        
        // Queue for async processing
        errorQueue.offer(report);
        
        // Notify player if applicable
        if (player != null && player.isOnline()) {
            notifyPlayer(player, severity);
        }
    }
    
    /**
     * Quick handle method with just error and context
     */
    public void handle(Throwable error, String context) {
        handle(error, context, null, null, -1, null, null, null);
    }
    
    /**
     * Classify error by type
     */
    private Severity classifyError(Throwable error) {
        String errorClass = error.getClass().getName();
        String message = error.getMessage();
        
        // CRITICAL errors
        if (error instanceof OutOfMemoryError ||
            errorClass.contains("NoClassDefFoundError") ||
            errorClass.contains("NoSuchMethodError") ||
            (message != null && message.contains("server has stopped responding"))) {
            return Severity.CRITICAL;
        }
        
        // WARNING errors
        if (error instanceof IOException ||
            error instanceof java.net.SocketTimeoutException ||
            errorClass.contains("InvocationTargetException") ||
            errorClass.contains("IllegalAccessException") ||
            (message != null && (
                message.contains("NMS") ||
                message.contains("reflection") ||
                message.contains("timeout")
            ))) {
            return Severity.WARNING;
        }
        
        // Default to INFO
        return Severity.INFO;
    }
    
    /**
     * Log error to console with appropriate level
     */
    private void logToConsole(ErrorReport report) {
        String msg = String.format("[%s] Error in %s: %s", 
            report.severity.getName(), 
            report.context, 
            report.error.getMessage());
        
        switch (report.severity) {
            case CRITICAL:
                plugin.getLogger().log(Level.SEVERE, msg, report.error);
                break;
            case WARNING:
                plugin.getLogger().log(Level.WARNING, msg, report.error);
                break;
            default:
                plugin.getLogger().log(Level.INFO, msg, report.error);
        }
    }
    
    /**
     * Notify player of error
     */
    private void notifyPlayer(Player player, Severity severity) {
        if (severity == Severity.CRITICAL) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                "[vUnstable] A critical error occurred! Staff have been notified.")
                .color(net.kyori.adventure.text.format.NamedTextColor.RED));
        } else if (severity == Severity.WARNING) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                "[vUnstable] An error occurred. Please try again.")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        }
    }
    
    /**
     * Start background error processor
     */
    private void startErrorProcessor() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            processErrorQueue();
        }, 20L, 100L); // Check every 5 seconds
    }
    
    /**
     * Process queued errors
     */
    private void processErrorQueue() {
        if (errorQueue.isEmpty()) return;
        
        ErrorReport report = errorQueue.poll();
        if (report == null) return;
        
        // Write to file and get the file reference
        File logFile = null;
        if (shouldLogToFile()) {
            logFile = writeToFile(report);
        }
        
        // Send to Discord if applicable (pass the log file)
        if (shouldSendToDiscord(report.severity)) {
            sendToDiscord(report, logFile);
        }
    }
    
    /**
     * Write detailed error report to file
     * @return The created log file, or null if failed
     */
    private File writeToFile(ErrorReport report) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date(report.timestamp));
            String filename = "error_" + timestamp + ".txt";
            File errorFile = new File(errorsDir, filename);
            
            // Archive old files if needed
            archiveOldFiles();
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(errorFile))) {
                writer.println("[ERROR REPORT - vUnstable v" + plugin.getDescription().getVersion() + "]");
                writer.println("Timestamp: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(report.timestamp)));
                writer.println("Server: " + Bukkit.getServer().getName());
                writer.println();
                
                writer.println("[SERVER SPECS]");
                writer.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
                writer.println("Java Version: " + System.getProperty("java.version"));
                writer.println("Server Software: " + Bukkit.getVersion());
                writer.println("Online Players: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
                
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                long maxMemory = runtime.maxMemory() / 1024 / 1024;
                writer.println("RAM Usage: " + usedMemory + "MB / " + maxMemory + "MB");
                
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                writer.println("CPU: " + osBean.getAvailableProcessors() + " cores");
                
                double[] tps = Bukkit.getServer().getTPS();
                if (tps.length > 0) {
                    writer.println("TPS: " + String.format("%.2f", tps[0]));
                }
                writer.println();
                
                writer.println("[ACTIVE PLUGINS]");
                StringBuilder plugins = new StringBuilder();
                for (org.bukkit.plugin.Plugin p : Bukkit.getPluginManager().getPlugins()) {
                    if (plugins.length() > 0) plugins.append(", ");
                    plugins.append(p.getName());
                }
                writer.println(plugins);
                writer.println();
                
                writer.println("[ERROR DETAILS]");
                writer.println("Severity: " + report.severity.getName());
                writer.println("Type: " + report.error.getClass().getName());
                writer.println("Message: " + report.error.getMessage());
                if (report.className != null) {
                    writer.println("Class: " + report.className);
                }
                if (report.methodName != null) {
                    writer.println("Method: " + report.methodName);
                }
                if (report.lineNumber > 0) {
                    writer.println("Line: " + report.lineNumber);
                }
                writer.println("Context: " + report.context);
                writer.println();
                
                writer.println("[STACK TRACE]");
                report.error.printStackTrace(writer);
                writer.println();
                
                writer.println("[CONTEXT]");
                if (report.player != null) {
                    writer.println("Player: " + report.player.getName() + " (" + report.player.getUniqueId() + ")");
                }
                if (report.location != null) {
                    writer.println("Location: " + report.location.getWorld().getName() + " @ " + 
                        report.location.getBlockX() + ", " + report.location.getBlockY() + ", " + report.location.getBlockZ());
                }
                if (report.additionalInfo != null) {
                    writer.println("Additional Info: " + report.additionalInfo);
                }
            }
            
            return errorFile;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write error log", e);
            return null;
        }
    }
    
    /**
     * Archive old log files
     */
    private void archiveOldFiles() {
        File[] files = errorsDir.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null) return;
        
        long now = System.currentTimeMillis();
        long maxAge = 7 * 24 * 60 * 60 * 1000; // 7 days
        
        for (File file : files) {
            if (now - file.lastModified() > maxAge) {
                File archiveFile = new File(archiveDir, file.getName());
                file.renameTo(archiveFile);
            }
            // Also check file size
            if (file.length() > 10 * 1024 * 1024) { // 10MB
                File archiveFile = new File(archiveDir, file.getName());
                file.renameTo(archiveFile);
            }
        }
    }
    
    /**
     * Send error to Discord webhook
     */
    private void sendToDiscord(ErrorReport report, File logFile) {
        long now = System.currentTimeMillis();
        long rateLimit = getRateLimitMinutes() * 60 * 1000;
        
        // Rate limiting (except for CRITICAL)
        if (report.severity != Severity.CRITICAL && (now - lastDiscordSend) < rateLimit) {
            plugin.getLogger().info("[vUnstable] Discord rate limit active, skipping send");
            return;
        }
        
        discordWebhook.sendError(report, logFile);
        lastDiscordSend = now;
    }
    
    /**
     * Check if error should be logged to file
     * Always true - local file logging is always enabled
     */
    private boolean shouldLogToFile() {
        return true;
    }
    
    /**
     * Check if error should be sent to Discord
     * Only checks the simple Error-Log boolean toggle
     * INFO level errors don't go to Discord
     */
    private boolean shouldSendToDiscord(Severity severity) {
        // INFO level doesn't go to Discord
        if (severity == Severity.INFO) return false;
        
        // Check simple boolean toggle
        return config.isErrorLogEnabled();
    }
    
    /**
     * Get rate limit - hardcoded to prevent modification
     */
    private int getRateLimitMinutes() {
        return 5; // Hardcoded 5 minute rate limit
    }
    
    /**
     * Generate test error for debugging
     */
    public void generateTestError() {
        try {
            int result = 1 / 0; // Division by zero
        } catch (ArithmeticException e) {
            handle(e, "ErrorHandler.generateTestError()", null, null, -1, null, null, "Test error for debugging webhook");
        }
    }
    
    /**
     * Get error statistics
     */
    public int getErrorCount() {
        return errorCount;
    }
    
    public long getSessionStartTime() {
        return sessionStartTime;
    }
    
    /**
     * Get recent error files
     */
    public File[] getRecentErrors(int limit) {
        File[] files = errorsDir.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null) return new File[0];
        
        // Sort by date (newest first)
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        // Return limited subset
        return java.util.Arrays.copyOf(files, Math.min(files.length, limit));
    }
}
