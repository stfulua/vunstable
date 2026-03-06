package com.vprolabs.vunstable.listener;

import com.vprolabs.vunstable.config.ConfigManager;
import com.vprolabs.vunstable.vUnstable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * EntityListener - Handles entity events and ground detection for Nuke TNT.
 * 
 * Features:
 * - Tick-based ground detection for instant Nuke detonation
 * - Prevents block drops from vUnstable explosions (but blocks still break)
 * - Tracks TNT via metadata
 */
public class EntityListener implements Listener {
    
    private final JavaPlugin plugin;
    private final Set<UUID> activeNukeWorlds = new HashSet<>();
    private MethodHandle nmsSetFuse;
    private boolean nmsAvailable = false;
    
    // Metadata keys
    public static final String META_NUKE_ROD = "vunstable_nuke_rod";
    public static final String META_STAB_ROD = "vunstable_stab_rod";
    public static final String META_NUKE_EXPLOSION = "vunstable_nuke_explosion";
    public static final String META_NUKE_SYNC_DELAY = "vunstable_nuke_sync_delay";
    public static final String META_NUKE_RING_INDEX = "vunstable_nuke_ring_index";
    public static final String META_NUKE_PENDING_EXPLOSION = "vunstable_nuke_pending";
    
    // Track pending synchronized explosions
    private final Set<UUID> pendingExplosions = new HashSet<>();
    
    public EntityListener(JavaPlugin plugin) {
        this.plugin = plugin;
        initMethodHandles();
        startGroundDetectionTask();
    }
    
    private void initMethodHandles() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> primedTnt = Class.forName("net.minecraft.world.entity.item.PrimedTnt");
            nmsSetFuse = lookup.findVirtual(primedTnt, "setFuse", MethodType.methodType(void.class, int.class));
            nmsAvailable = true;
        } catch (Exception e) {
            plugin.getLogger().warning("[vUnstable] NMS fuse setter not available, using Bukkit fallback");
            nmsAvailable = false;
        }
    }
    
    /**
     * Tick-based ground detection task - runs every tick to check all TNTPrimed entities.
     * More reliable than EntityMoveEvent for fast-moving entities.
     */
    private void startGroundDetectionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Only check worlds that have had Nuke TNT spawned
                for (UUID worldId : new HashSet<>(activeNukeWorlds)) {
                    World world = Bukkit.getWorld(worldId);
                    if (world == null) continue;
                    
                    boolean foundAny = false;
                    
                    for (TNTPrimed tnt : world.getEntitiesByClass(TNTPrimed.class)) {
                        // Check if this is our Nuke TNT
                        if (!hasMetadata(tnt, META_NUKE_ROD)) continue;
                        
                        foundAny = true;
                        
                        // Check if on ground OR velocity is near zero (falling stopped)
                        boolean onGround = tnt.isOnGround();
                        double velY = Math.abs(tnt.getVelocity().getY());
                        boolean stoppedFalling = velY < 0.1 && tnt.getTicksLived() > 5;
                        
                        if (onGround || stoppedFalling) {
                            // BOUNCE PREVENTION: Freeze TNT immediately on ground hit
                            freezeTNT(tnt);
                            
                            // Check for synchronized explosion delay
                            int syncDelay = getSyncDelay(tnt);
                            
                            if (syncDelay > 0 && !hasMetadata(tnt, META_NUKE_PENDING_EXPLOSION)) {
                                // Schedule delayed explosion for synchronization
                                tnt.setMetadata(META_NUKE_PENDING_EXPLOSION, new FixedMetadataValue(plugin, true));
                                pendingExplosions.add(tnt.getUniqueId());
                                
                                int ringIndex = getRingIndex(tnt);
                                plugin.getLogger().fine("[vUnstable] Nuke Ring " + ringIndex + ": Ground hit, scheduling sync explosion in " + syncDelay + " ticks");
                                
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (tnt.isDead() || !tnt.isValid()) {
                                            pendingExplosions.remove(tnt.getUniqueId());
                                            cancel();
                                            return;
                                        }
                                        
                                        // Remove metadata and explode
                                        tnt.removeMetadata(META_NUKE_ROD, plugin);
                                        tnt.removeMetadata(META_NUKE_PENDING_EXPLOSION, plugin);
                                        pendingExplosions.remove(tnt.getUniqueId());
                                        
                                        boolean success = detonateTNT(tnt);
                                        if (success) {
                                            plugin.getLogger().fine("[vUnstable] Nuke Ring " + ringIndex + ": Synchronized explosion executed");
                                        }
                                    }
                                }.runTaskLater(plugin, syncDelay);
                                
                            } else if (syncDelay <= 0) {
                                // No sync delay - explode immediately
                                tnt.removeMetadata(META_NUKE_ROD, plugin);
                                boolean success = detonateTNT(tnt);
                                
                                if (success) {
                                    plugin.getLogger().fine("[vUnstable] Nuke TNT detonated at " + 
                                        tnt.getLocation().getBlockX() + "," + 
                                        tnt.getLocation().getBlockY() + "," + 
                                        tnt.getLocation().getBlockZ() + " (ground detected)");
                                }
                            }
                        }
                    }
                    
                    // Remove world from active set if no more Nuke TNT found
                    if (!foundAny) {
                        activeNukeWorlds.remove(worldId);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Detonates TNT by setting fuse to 0.
     * Uses NMS if available, falls back to Bukkit API.
     */
    private boolean detonateTNT(TNTPrimed tnt) {
        // Try NMS first for instant detonation
        if (nmsAvailable) {
            try {
                Object nmsTnt = tnt.getClass().getMethod("getHandle").invoke(tnt);
                nmsSetFuse.invoke(nmsTnt, 0);
                return true;
            } catch (Throwable ex) {
                // NMS failed, fall through to Bukkit
                plugin.getLogger().fine("[vUnstable] NMS detonation failed, using Bukkit fallback");
            }
        }
        
        // Bukkit fallback
        try {
            tnt.setFuseTicks(0);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().severe("[vUnstable] Failed to detonate TNT: " + ex.getMessage());
            return false;
        }
    }
    
    /**
     * Mark a world as having active Nuke TNT.
     * Called by AsyncSpawnEngine when spawning Nuke TNT.
     */
    public void markNukeActive(World world) {
        if (world != null) {
            activeNukeWorlds.add(world.getUID());
        }
    }
    
    /**
     * Check if entity has our metadata key.
     */
    private boolean hasMetadata(TNTPrimed tnt, String key) {
        if (!tnt.hasMetadata(key)) return false;
        for (MetadataValue mv : tnt.getMetadata(key)) {
            if (mv.getOwningPlugin() == plugin) return true;
        }
        return false;
    }
    
    /**
     * Get sync delay from TNT metadata.
     */
    private int getSyncDelay(TNTPrimed tnt) {
        if (!tnt.hasMetadata(META_NUKE_SYNC_DELAY)) return 0;
        for (MetadataValue mv : tnt.getMetadata(META_NUKE_SYNC_DELAY)) {
            if (mv.getOwningPlugin() == plugin) {
                return mv.asInt();
            }
        }
        return 0;
    }
    
    /**
     * Get ring index from TNT metadata.
     */
    private int getRingIndex(TNTPrimed tnt) {
        if (!tnt.hasMetadata(META_NUKE_RING_INDEX)) return -1;
        for (MetadataValue mv : tnt.getMetadata(META_NUKE_RING_INDEX)) {
            if (mv.getOwningPlugin() == plugin) {
                return mv.asInt();
            }
        }
        return -1;
    }
    
    /**
     * FREEZE TNT to prevent bouncing.
     * Zeroes velocity and locks position.
     */
    private void freezeTNT(TNTPrimed tnt) {
        try {
            // Method 1: Bukkit API - zero velocity
            tnt.setVelocity(new Vector(0, 0, 0));
            
            // Method 2: NMS freeze (if available)
            try {
                Object nmsTnt = tnt.getClass().getMethod("getHandle").invoke(tnt);
                
                // Set delta movement to zero via reflection
                java.lang.reflect.Method setDeltaMovement = nmsTnt.getClass().getMethod("setDeltaMovement", double.class, double.class, double.class);
                setDeltaMovement.invoke(nmsTnt, 0.0, 0.0, 0.0);
                
                // Disable physics impulses
                java.lang.reflect.Field hasImpulseField = nmsTnt.getClass().getField("hasImpulse");
                hasImpulseField.setBoolean(nmsTnt, false);
                
                plugin.getLogger().fine("[vUnstable] TNT frozen via NMS");
            } catch (Exception nmsEx) {
                // NMS failed, Bukkit velocity zero is sufficient
                plugin.getLogger().fine("[vUnstable] TNT frozen via Bukkit API");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[vUnstable] Failed to freeze TNT: " + e.getMessage());
        }
    }
    
    /**
     * Handle EntityExplodeEvent - mark blocks for no drops.
     * We do NOT cancel the event - we want blocks to break, just not drop items.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;
        
        // Check if this is our TNT
        boolean isNuke = hasMetadata(tnt, META_NUKE_ROD);
        boolean isStab = hasMetadata(tnt, META_STAB_ROD);
        
        if (!isNuke && !isStab) return;
        
        String type = isNuke ? "Nuke" : "Stab";
        int blockCount = event.blockList().size();
        
        plugin.getLogger().fine("[vUnstable] " + type + " explosion processing: " + blockCount + " blocks");
        
        // Mark all blocks in explosion for no drops
        for (Block block : event.blockList()) {
            block.setMetadata(META_NUKE_EXPLOSION, new FixedMetadataValue(plugin, true));
        }
        
        // Ensure yield is proper (in case it wasn't set during spawn)
        event.setYield(4.0f);
    }
    
    /**
     * Prevent item drops from blocks broken by our explosions.
     * This is Paper/Spigot 1.21+ specific event.
     * Only cancels if config remove-block-drops is true (default).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDropItem(BlockDropItemEvent event) {
        // Check config - if false, allow vanilla drops
        if (!ConfigManager.getInstance().shouldRemoveBlockDrops()) {
            return;
        }
        
        Block block = event.getBlock();
        
        // Check if this block was marked by our explosion
        if (!block.hasMetadata(META_NUKE_EXPLOSION)) return;
        
        // Cancel the drops
        event.setCancelled(true);
        
        // Clean up metadata
        block.removeMetadata(META_NUKE_EXPLOSION, plugin);
        
        plugin.getLogger().fine("[vUnstable] Cancelled item drops for block at " + block.getLocation());
    }
    
    /**
     * Fallback for older versions: manually break blocks without drops.
     * This runs after explosion to ensure blocks are broken without drops.
     * Only runs if config remove-block-drops is true (default).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplodeMonitor(EntityExplodeEvent event) {
        // Check config - if false, allow vanilla drops
        if (!ConfigManager.getInstance().shouldRemoveBlockDrops()) {
            return;
        }
        
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;
        
        boolean isNuke = hasMetadata(tnt, META_NUKE_ROD);
        boolean isStab = hasMetadata(tnt, META_STAB_ROD);
        
        if (!isNuke && !isStab) return;
        
        // For any remaining blocks that might drop items, force them to air
        // This is a safety net for versions without BlockDropItemEvent
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : event.blockList()) {
                    if (block.hasMetadata(META_NUKE_EXPLOSION)) {
                        block.setType(org.bukkit.Material.AIR, false);
                        block.removeMetadata(META_NUKE_EXPLOSION, plugin);
                    }
                }
            }
        }.runTask(plugin);
    }
}
