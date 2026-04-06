package com.vprolabs.vunstable.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * ErrorHandler - Simple error logging to console.
 * 
 * v1.2.0: Removed external reporting. Errors are logged to console only.
 * If issues occur, please contact us at: discord.gg/SNzUYWbc5Q
 */
public class ErrorHandler {
    
    private static ErrorHandler instance;
    private final JavaPlugin plugin;
    private final List<ErrorEntry> recentErrors;
    private static final int MAX_RECENT_ERRORS = 10;
    
    /**
     * Represents a recent error entry
     */
    public static class ErrorEntry {
        public final Throwable error;
        public final String context;
        public final long timestamp;
        public final String playerName;
        
        public ErrorEntry(Throwable error, String context, String playerName) {
            this.error = error;
            this.context = context;
            this.playerName = playerName;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public ErrorHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.recentErrors = new ArrayList<>();
        instance = this;
    }
    
    public static ErrorHandler getInstance() {
        return instance;
    }
    
    /**
     * Handle error - log to console and track for admin notification
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
        
        String playerName = player != null ? player.getName() : "N/A";
        
        // Log to console
        logToConsole(error, context, className, methodName, playerName, additionalInfo);
        
        // Track for admin notification
        trackError(error, context, playerName);
    }
    
    /**
     * Quick handle method with just error and context
     */
    public void handle(Throwable error, String context) {
        handle(error, context, null, null, -1, null, null, null);
    }
    
    /**
     * Log error to console with detailed information
     */
    private void logToConsole(Throwable error, String context, String className, String methodName, 
                              String playerName, String additionalInfo) {
        plugin.getLogger().log(Level.SEVERE, "[vUnstable Error] Context: " + context);
        plugin.getLogger().log(Level.SEVERE, "[vUnstable Error] Player: " + playerName);
        if (className != null) {
            plugin.getLogger().log(Level.SEVERE, "[vUnstable Error] Class: " + className);
        }
        if (methodName != null) {
            plugin.getLogger().log(Level.SEVERE, "[vUnstable Error] Method: " + methodName);
        }
        if (additionalInfo != null) {
            plugin.getLogger().log(Level.SEVERE, "[vUnstable Error] Info: " + additionalInfo);
        }
        plugin.getLogger().log(Level.SEVERE, "[vUnstable Error] Exception:", error);
        plugin.getLogger().log(Level.SEVERE, "[vUnstable Error] If this issue persists, please contact us: discord.gg/SNzUYWbc5Q");
    }
    
    /**
     * Track error for admin notification
     */
    private void trackError(Throwable error, String context, String playerName) {
        ErrorEntry entry = new ErrorEntry(error, context, playerName);
        
        synchronized (recentErrors) {
            recentErrors.add(0, entry); // Add to front
            if (recentErrors.size() > MAX_RECENT_ERRORS) {
                recentErrors.remove(recentErrors.size() - 1); // Remove oldest
            }
        }
    }
    
    /**
     * Get recent errors for admin notification
     */
    public List<ErrorEntry> getRecentErrors() {
        synchronized (recentErrors) {
            return new ArrayList<>(recentErrors);
        }
    }
    
    /**
     * Clear recent errors (called after admin is notified)
     */
    public void clearRecentErrors() {
        synchronized (recentErrors) {
            recentErrors.clear();
        }
    }
    
    /**
     * Check if there are errors to report
     */
    public boolean hasErrors() {
        synchronized (recentErrors) {
            return !recentErrors.isEmpty();
        }
    }
    
    /**
     * Get error count
     */
    public int getErrorCount() {
        synchronized (recentErrors) {
            return recentErrors.size();
        }
    }
}
