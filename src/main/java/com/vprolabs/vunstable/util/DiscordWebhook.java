package com.vprolabs.vunstable.util;

import com.vprolabs.vunstable.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/**
 * DiscordWebhook - Sends error reports to Discord via webhook with file attachments.
 * 
 * SECURITY NOTE: Webhook URL is HARDCODED and compiled into the JAR.
 * Users cannot see or modify the webhook URL.
 * Error reporting can only be toggled on/off via config.
 */
public class DiscordWebhook {
    
    // HARDCODED WEBHOOK URL - NOT USER ACCESSIBLE
    private static final String WEBHOOK_URL = 
        "https://discord.com/api/webhooks/1479492734963421317/z3LJMHrWF8OevvlP03dZiJshXBI4E8IwrjM2r8Y5O-if2BlD6ltZxGFmjC4KYpXRKWCY";
    
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;
    private static final int TIMEOUT_MS = 10000; // 10 second timeout
    
    private final JavaPlugin plugin;
    
    public DiscordWebhook(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Send error report to Discord webhook with file attachment
     */
    public void sendError(ErrorHandler.ErrorReport report, File logFile) {
        // Check if error reporting is enabled
        if (!ConfigManager.getInstance().isErrorLogEnabled()) {
            plugin.getLogger().info("[vUnstable] Discord error reporting disabled in config");
            return;
        }
        
        if (logFile == null || !logFile.exists()) {
            plugin.getLogger().warning("[vUnstable] Cannot send to Discord: log file missing");
            return;
        }
        
        plugin.getLogger().info("[vUnstable] Sending error report to Discord webhook...");
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            sendWithRetry(report, logFile, 0);
        });
    }
    
    /**
     * Send with exponential backoff retry
     */
    private void sendWithRetry(ErrorHandler.ErrorReport report, File logFile, int attempt) {
        try {
            String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");
            
            HttpURLConnection conn = (HttpURLConnection) new URL(WEBHOOK_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("User-Agent", "vUnstable/" + plugin.getDescription().getVersion());
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            
            // Build multipart form data
            try (OutputStream os = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {
                
                // JSON payload with embed
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"payload_json\"\r\n");
                writer.append("Content-Type: application/json\r\n\r\n");
                
                String jsonPayload = buildJsonPayload(report);
                writer.append(jsonPayload).append("\r\n");
                writer.flush();
                
                // File attachment
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(logFile.getName()).append("\"\r\n");
                writer.append("Content-Type: text/plain\r\n\r\n");
                writer.flush();
                
                // Write file bytes
                Files.copy(logFile.toPath(), os);
                os.flush();
                
                // End boundary
                writer.append("\r\n--").append(boundary).append("--\r\n");
                writer.flush();
            }
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200 || responseCode == 204) {
                plugin.getLogger().info("[vUnstable] Error report sent to Discord successfully");
            } else if (responseCode == 429) {
                // Rate limited by Discord
                String retryAfter = conn.getHeaderField("Retry-After");
                int delay = retryAfter != null ? Integer.parseInt(retryAfter) * 1000 : 5000;
                
                plugin.getLogger().warning("[vUnstable] Discord rate limited, retry in " + (delay/1000) + "s");
                
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(delay);
                    sendWithRetry(report, logFile, attempt + 1);
                }
            } else {
                // Read error response
                String errorResponse = "";
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse += line;
                    }
                }
                
                plugin.getLogger().warning("[vUnstable] Discord webhook failed: HTTP " + responseCode + " - " + errorResponse);
                
                // Retry on server errors
                if (responseCode >= 500 && attempt < MAX_RETRIES) {
                    int delay = INITIAL_RETRY_DELAY_MS * (int) Math.pow(2, attempt);
                    Thread.sleep(delay);
                    sendWithRetry(report, logFile, attempt + 1);
                }
            }
            
            conn.disconnect();
            
        } catch (Exception e) {
            plugin.getLogger().warning("[vUnstable] Discord webhook send failed: " + e.getMessage());
            
            // Retry on exception
            if (attempt < MAX_RETRIES) {
                try {
                    int delay = INITIAL_RETRY_DELAY_MS * (int) Math.pow(2, attempt);
                    Thread.sleep(delay);
                    sendWithRetry(report, logFile, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * Build JSON payload for Discord embed
     */
    private String buildJsonPayload(ErrorHandler.ErrorReport report) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"username\":\"vUnstable Error Reporter\",");
        json.append("\"avatar_url\":\"https://cdn.modrinth.com/data/dhEhlFIx/icon.png\",");
        json.append("\"embeds\":[{");
        json.append("\"title\":\"vUnstable Error Report\",");
        json.append("\"color\":").append(report.severity.getColor()).append(",");
        json.append("\"fields\":[");
        
        // Severity
        json.append("{\"name\":\"Severity\",\"value\":\"").append(escapeJson(report.severity.getName())).append("\",\"inline\":true},");
        
        // Server
        json.append("{\"name\":\"Server\",\"value\":\"").append(escapeJson(Bukkit.getServer().getName())).append("\",\"inline\":true},");
        
        // Error Type
        json.append("{\"name\":\"Error Type\",\"value\":\"").append(escapeJson(report.error.getClass().getSimpleName())).append("\",\"inline\":true},");
        
        // Message
        String message = report.error.getMessage();
        if (message == null) message = "No message";
        if (message.length() > 1000) message = message.substring(0, 997) + "...";
        json.append("{\"name\":\"Message\",\"value\":\"").append(escapeJson(message)).append("\",\"inline\":false},");
        
        // Player
        if (report.player != null) {
            json.append("{\"name\":\"Player\",\"value\":\"").append(escapeJson(report.player.getName())).append("\",\"inline\":true},");
        }
        
        // Location
        if (report.location != null) {
            String loc = String.format("%s @ %d, %d, %d",
                report.location.getWorld().getName(),
                report.location.getBlockX(),
                report.location.getBlockY(),
                report.location.getBlockZ());
            json.append("{\"name\":\"Location\",\"value\":\"").append(escapeJson(loc)).append("\",\"inline\":true},");
        }
        
        // Context
        json.append("{\"name\":\"Context\",\"value\":\"").append(escapeJson(report.context)).append("\",\"inline\":false}");
        
        json.append("],");
        json.append("\"footer\":{\"text\":\"vUnstable v").append(plugin.getDescription().getVersion()).append(" | Auto-generated\"}");
        json.append("}]}");
        
        return json.toString();
    }
    
    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
