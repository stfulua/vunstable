package com.vprolabs.vunstable.rod.impl;

import com.vprolabs.vunstable.config.ConfigManager;
import com.vprolabs.vunstable.engine.AsyncSpawnEngine;
import com.vprolabs.vunstable.rod.RodManager;
import com.vprolabs.vunstable.vUnstable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NukeRod - The Orbital Strike Cannon.
 * 
 * Spawns 2000 TNT in 10 rings with synchronized detonation.
 * Uses reliable UUID tracking to prevent frozen TNT.
 */
public class NukeRod implements RodManager.Rod {
    
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final AsyncSpawnEngine spawnEngine;
    
    // RELIABLE TRACKING: Static maps for all Nuke TNT across the server
    private static final Map<UUID, Long> activeNukes = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> nukeSyncDelays = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> nukeRingIndices = new ConcurrentHashMap<>();
    private static final AtomicInteger activeNukeCount = new AtomicInteger(0);
    
    private static final int MAX_CONCURRENT_NUKES = 1; // Prevent overload
    private static final int FORCE_EXPLODE_MS = 5000; // 5 seconds timeout
    private static final int NUKE_COOLDOWN_TICKS = 120; // 6 seconds cleanup
    
    private MethodHandle nmsSetFuse;
    private boolean nmsAvailable = false;
    
    public NukeRod(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ConfigManager.getInstance();
        this.spawnEngine = ((vUnstable) plugin).getSpawnEngine();
        initMethodHandles();
        startExplosionMonitor();
    }
    
    private void initMethodHandles() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> primedTnt = Class.forName("net.minecraft.world.entity.item.PrimedTnt");
            nmsSetFuse = lookup.findVirtual(primedTnt, "setFuse", MethodType.methodType(void.class, int.class));
            nmsAvailable = true;
        } catch (Exception e) {
            plugin.getLogger().warning("[vUnstable] NukeRod: NMS fuse setter not available, using Bukkit fallback");
            nmsAvailable = false;
        }
    }
    
    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text("Nuke").color(TextColor.color(0xFF0000)));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            item.setItemMeta(meta);
        }
        
        item.setDurability((short) 63);
        return item;
    }
    
    @Override
    public void activate(Location target, Player player) {
        // CONCURRENT NUKE LIMIT - Prevent overload
        if (activeNukeCount.get() >= MAX_CONCURRENT_NUKES) {
            if (player != null) {
                player.sendMessage(net.kyori.adventure.text.Component.text(
                    "[vUnstable] Wait for current nuke to finish! (" + activeNukes.size() + " TNT active)")
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
            plugin.getLogger().warning("[vUnstable] Nuke blocked: Another nuke is currently active (" + activeNukes.size() + " TNT)");
            return;
        }
        
        try {
            double centerX = target.getX();
            double centerZ = target.getZ();
            double spawnY = target.getY() + config.getNukeSpawnHeight();
            
            plugin.getLogger().info("[vUnstable] NUKE ACTIVATED at X" + centerX + " Z" + centerZ + " Y" + spawnY);
            
            spawnEngine.clearSpawnedEntities();
            
            // Increment active count
            activeNukeCount.incrementAndGet();
            
            // Get optimized spawn parameters
            var params = vUnstable.getNukeParams();
            int ratePerTick = params.ratePerTick();
            
            int rings = config.getNukeRings();
            double minRadius = config.getNukeMinRadius();
            double maxRadius = config.getNukeMaxRadius();
            double radiusStep = (maxRadius - minRadius) / (rings - 1);
            int totalTnt = config.getNukeTotalTnt();
            int tntPerRing = totalTnt / rings;
            
            // Calculate spawn timing
            int tntPerTick = ratePerTick;
            int ticksBetweenRings = Math.max(1, (tntPerRing + tntPerTick - 1) / tntPerTick);
            
            // Synchronized explosion settings
            boolean syncEnabled = config.isNukeSyncExplosions();
            int baseDelay = config.getNukeBaseDelayTicks();
            
            plugin.getLogger().info("[vUnstable] Spawning " + totalTnt + " TNT in " + rings + 
                " rings | Rate: " + ratePerTick + "/tick | Sync: " + syncEnabled + 
                " | Active nukes: " + activeNukeCount.get() + "/" + MAX_CONCURRENT_NUKES);
            
            // Spawn all rings
            for (int ring = 0; ring < rings; ring++) {
                double radius = minRadius + (ring * radiusStep);
                
                // Calculate sync delay
                int syncDelay = 0;
                if (syncEnabled) {
                    syncDelay = ((rings - 1) - ring) * ticksBetweenRings + baseDelay;
                }
                
                final int finalRing = ring;
                final int finalSyncDelay = syncDelay;
                final int ringNum = ring + 1;
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        // Queue spawns and track UUIDs
                        List<UUID> spawned = spawnEngine.queueRingSpawnsTracked(
                            centerX, centerZ, spawnY, radius, tntPerRing,
                            config.getNukeVelocityY(), config.getNukeFuseTicks(), 
                            target.getWorld(), finalRing, finalSyncDelay);
                        
                        // Track all spawned TNT
                        for (UUID id : spawned) {
                            activeNukes.put(id, System.currentTimeMillis());
                            if (syncEnabled) {
                                nukeSyncDelays.put(id, finalSyncDelay);
                                nukeRingIndices.put(id, finalRing);
                            }
                        }
                        
                        plugin.getLogger().fine("[vUnstable] Ring " + ringNum + "/" + rings + 
                            " spawned " + spawned.size() + " TNT (sync delay: " + finalSyncDelay + ")");
                            
                    } catch (Exception e) {
                        com.vprolabs.vunstable.util.ErrorHandler.getInstance().handle(e, 
                            "NukeRod.activate() [ring " + ringNum + "]", 
                            "NukeRod", "activate", 82, player, target, 
                            "Failed to queue ring " + ringNum);
                    }
                }, ring * ticksBetweenRings);
            }
            
            // Schedule cleanup after max time
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                int remaining = activeNukes.size();
                if (remaining > 0) {
                    plugin.getLogger().warning("[vUnstable] Nuke cleanup: Force exploding " + remaining + " remaining TNT");
                    forceExplodeAll();
                }
                activeNukeCount.decrementAndGet();
                plugin.getLogger().info("[vUnstable] Nuke complete. Active nukes: " + activeNukeCount.get() + "/" + MAX_CONCURRENT_NUKES);
            }, NUKE_COOLDOWN_TICKS);
            
            // Sync message
            if (syncEnabled) {
                int maxDelay = ((rings - 1) - 0) * ticksBetweenRings + baseDelay;
                double seconds = maxDelay / 20.0;
                plugin.getLogger().info("[vUnstable] All nukes synchronized to explode in ~" + String.format("%.1f", seconds) + "s");
            }
            
        } catch (Exception e) {
            activeNukeCount.decrementAndGet();
            com.vprolabs.vunstable.util.ErrorHandler.getInstance().handle(e, 
                "NukeRod.activate()", "NukeRod", "activate", 82, player, target, 
                "2000 TNT Nuke spawn attempt");
            
            if (player != null && player.isOnline()) {
                player.sendMessage(net.kyori.adventure.text.Component.text(
                    "[vUnstable] An error occurred while activating the Nuke rod! Staff have been notified.")
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        }
    }
    
    /**
     * Start the explosion monitor task - runs every tick to check all tracked TNT.
     */
    private void startExplosionMonitor() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<UUID, Long>> iter = activeNukes.entrySet().iterator();
                
                while (iter.hasNext()) {
                    Map.Entry<UUID, Long> entry = iter.next();
                    UUID id = entry.getKey();
                    long spawnTime = entry.getValue();
                    
                    // Get entity
                    org.bukkit.entity.Entity entity = Bukkit.getEntity(id);
                    
                    // Check if entity is valid
                    if (entity == null || entity.isDead() || !(entity instanceof TNTPrimed tnt)) {
                        iter.remove();
                        nukeSyncDelays.remove(id);
                        nukeRingIndices.remove(id);
                        continue;
                    }
                    
                    long aliveTime = now - spawnTime;
                    
                    // FORCE EXPLODE after 5 seconds NO MATTER WHAT
                    if (aliveTime > FORCE_EXPLODE_MS) {
                        plugin.getLogger().warning("[vUnstable] Force exploding timed-out TNT " + id + " (alive " + aliveTime + "ms)");
                        forceExplode(tnt);
                        iter.remove();
                        nukeSyncDelays.remove(id);
                        nukeRingIndices.remove(id);
                        continue;
                    }
                    
                    // Check if on ground and not already scheduled
                    if (tnt.isOnGround() && !tnt.hasMetadata("grounded")) {
                        tnt.setMetadata("grounded", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                        
                        // FREEZE immediately to prevent bounce
                        freezeTNT(tnt);
                        
                        // Get sync delay
                        int delay = nukeSyncDelays.getOrDefault(id, 0);
                        int ringIndex = nukeRingIndices.getOrDefault(id, -1);
                        
                        plugin.getLogger().fine("[vUnstable] Ring " + ringIndex + " TNT grounded, scheduling explosion in " + delay + " ticks");
                        
                        // Schedule explosion
                        if (delay > 0) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (tnt.isValid() && !tnt.isDead()) {
                                        forceExplode(tnt);
                                    }
                                }
                            }.runTaskLater(plugin, delay);
                        } else {
                            forceExplode(tnt);
                        }
                        
                        // Remove from active tracking (now scheduled)
                        iter.remove();
                        nukeSyncDelays.remove(id);
                        nukeRingIndices.remove(id);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // EVERY TICK
        
        // Cleanup task for orphaned entries (every 10 seconds)
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                int cleaned = 0;
                
                Iterator<Map.Entry<UUID, Long>> iter = activeNukes.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<UUID, Long> entry = iter.next();
                    if (now - entry.getValue() > 10000) { // 10 seconds old
                        org.bukkit.entity.Entity entity = Bukkit.getEntity(entry.getKey());
                        if (entity instanceof TNTPrimed tnt) {
                            plugin.getLogger().warning("[vUnstable] Cleaning up orphaned TNT " + entry.getKey());
                            forceExplode(tnt);
                            cleaned++;
                        }
                        iter.remove();
                        nukeSyncDelays.remove(entry.getKey());
                        nukeRingIndices.remove(entry.getKey());
                    }
                }
                
                if (cleaned > 0) {
                    plugin.getLogger().info("[vUnstable] Cleaned up " + cleaned + " orphaned TNT entities");
                }
            }
        }.runTaskTimer(plugin, 200L, 200L); // Every 10 seconds
    }
    
    /**
     * FORCE EXPLOSION - Never fails, multiple fallbacks
     */
    private void forceExplode(TNTPrimed tnt) {
        try {
            // Try NMS first
            Object nmsTnt = tnt.getClass().getMethod("getHandle").invoke(tnt);
            if (nmsSetFuse != null) {
                try {
                    nmsSetFuse.invoke(nmsTnt, 0);
                    return;
                } catch (Throwable ignored) {}
            }
        } catch (Exception ignored) {}
        
        try {
            // Fallback 1: Bukkit API
            tnt.setFuseTicks(0);
        } catch (Exception e2) {
            try {
                // Fallback 2: Create explosion at location
                Location loc = tnt.getLocation();
                tnt.remove();
                loc.getWorld().createExplosion(loc, 4.0f, false, true);
            } catch (Exception e3) {
                plugin.getLogger().severe("[vUnstable] CRITICAL: All explosion methods failed for TNT " + tnt.getUniqueId());
            }
        }
    }
    
    /**
     * Freeze TNT to prevent bouncing.
     */
    private void freezeTNT(TNTPrimed tnt) {
        try {
            tnt.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            
            // Try NMS freeze
            try {
                Object nmsTnt = tnt.getClass().getMethod("getHandle").invoke(tnt);
                java.lang.reflect.Method setDelta = nmsTnt.getClass().getMethod("setDeltaMovement", double.class, double.class, double.class);
                setDelta.invoke(nmsTnt, 0.0, 0.0, 0.0);
                java.lang.reflect.Field hasImpulse = nmsTnt.getClass().getField("hasImpulse");
                hasImpulse.setBoolean(nmsTnt, false);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            plugin.getLogger().warning("[vUnstable] Failed to freeze TNT: " + e.getMessage());
        }
    }
    
    /**
     * Force explode all tracked TNT (cleanup).
     */
    private void forceExplodeAll() {
        for (UUID id : new ArrayList<>(activeNukes.keySet())) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(id);
            if (entity instanceof TNTPrimed tnt) {
                forceExplode(tnt);
            }
        }
        activeNukes.clear();
        nukeSyncDelays.clear();
        nukeRingIndices.clear();
    }
    
    @Override
    public String getName() {
        return "Nuke";
    }
    
    // Public accessors for tracking
    public static int getActiveNukeCount() {
        return activeNukeCount.get();
    }
    
    public static int getTrackedTNTCount() {
        return activeNukes.size();
    }
}
