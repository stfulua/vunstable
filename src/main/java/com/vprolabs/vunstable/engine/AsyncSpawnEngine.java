package com.vprolabs.vunstable.engine;

import com.vprolabs.vunstable.config.ConfigManager;
import com.vprolabs.vunstable.listener.EntityListener;
import com.vprolabs.vunstable.vUnstable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * AsyncSpawnEngine - Handles async TNT spawning with rate limiting.
 * 
 * Features:
 * - Queue-based spawning (200 TNT per tick max)
 * - MethodHandles for fast NMS reflection
 * - Metadata tagging for Nuke/Stab identification
 * - Proper yield setting for block damage
 * - Fallback to Bukkit API if NMS fails
 */
public class AsyncSpawnEngine {
    
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final Deque<SpawnRequest> spawnQueue;
    private final List<Object> spawnedNmsEntities;
    private final List<TNTPrimed> spawnedBukkitEntities;
    private BukkitTask spawnTask;
    private EntityListener entityListener;
    
    // NMS MethodHandles
    private MethodHandle getHandle;
    private MethodHandle addEntity;
    private MethodHandle setDeltaMovement;
    private MethodHandle setFuse;
    private MethodHandle setYield;
    private Object tntConstructor;
    private boolean nmsAvailable = false;
    
    public AsyncSpawnEngine(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ConfigManager.getInstance();
        this.spawnQueue = new ArrayDeque<>();
        this.spawnedNmsEntities = new ArrayList<>();
        this.spawnedBukkitEntities = new ArrayList<>();
        initNMS();
        startSpawnTask();
    }
    
    public void setEntityListener(EntityListener listener) {
        this.entityListener = listener;
    }
    
    /**
     * Initialize NMS MethodHandles for fast reflection.
     */
    private void initNMS() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            
            // CraftWorld.getHandle()
            Class<?> craftWorld = Class.forName("org.bukkit.craftbukkit.CraftWorld");
            getHandle = lookup.findVirtual(craftWorld, "getHandle", MethodType.methodType(Class.forName("net.minecraft.server.level.ServerLevel")));
            
            // PrimedTnt constructor
            Class<?> primedTnt = Class.forName("net.minecraft.world.entity.item.PrimedTnt");
            Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
            
            for (var ctor : primedTnt.getConstructors()) {
                var params = ctor.getParameterTypes();
                if (params.length == 5 && params[0] == levelClass) {
                    tntConstructor = lookup.unreflectConstructor(ctor);
                    break;
                }
            }
            
            if (tntConstructor == null) {
                plugin.getLogger().warning("[vUnstable] No 5-param TNT constructor found");
                return;
            }
            
            // Methods
            setDeltaMovement = lookup.findVirtual(primedTnt, "setDeltaMovement", MethodType.methodType(void.class, double.class, double.class, double.class));
            setFuse = lookup.findVirtual(primedTnt, "setFuse", MethodType.methodType(void.class, int.class));
            
            // Try to find yield method (may not exist in all versions)
            try {
                setYield = lookup.findVirtual(primedTnt, "setYield", MethodType.methodType(void.class, float.class));
            } catch (Exception e) {
                plugin.getLogger().fine("[vUnstable] NMS setYield not available");
            }
            
            Class<?> serverLevel = Class.forName("net.minecraft.server.level.ServerLevel");
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> spawnReason = Class.forName("org.bukkit.event.entity.CreatureSpawnEvent$SpawnReason");
            addEntity = lookup.findVirtual(serverLevel, "addFreshEntity", MethodType.methodType(boolean.class, entityClass, spawnReason));
            
            nmsAvailable = true;
            plugin.getLogger().info("[vUnstable] AsyncSpawnEngine: NMS Mode (MethodHandles)");
        } catch (Exception e) {
            plugin.getLogger().warning("[vUnstable] AsyncSpawnEngine: Bukkit Mode - " + e.getMessage());
            nmsAvailable = false;
        }
    }
    
    /**
     * Start the spawn task that processes the queue.
     * Uses optimized rate from SpigotOptimizer.
     */
    private void startSpawnTask() {
        // Use optimized rate from SpigotOptimizer if available
        int rate = config.getMaxTntPerTick();
        var params = vUnstable.getNukeParams();
        if (params != null) {
            rate = params.ratePerTick();
        }
        
        final int spawnRate = rate;
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                processQueue(spawnRate);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Process up to 'max' spawns from the queue.
     */
    private void processQueue(int max) {
        int processed = 0;
        while (processed < max && !spawnQueue.isEmpty()) {
            SpawnRequest req = spawnQueue.poll();
            if (req != null) {
                try {
                    spawnTNT(req);
                    processed++;
                } catch (Exception e) {
                    com.vprolabs.vunstable.util.ErrorHandler.getInstance().handle(e, 
                        "AsyncSpawnEngine.processQueue()", 
                        "AsyncSpawnEngine", "processQueue", 142,
                        null, req.location, 
                        "TNT spawn failed at " + req.location);
                }
            }
        }
    }
    
    /**
     * Spawn a single TNT entity with proper metadata and yield.
     */
    private void spawnTNT(SpawnRequest req) {
        if (nmsAvailable) {
            try {
                Object level = getHandle.invoke(req.location.getWorld());
                Object tnt = ((MethodHandle) tntConstructor).invoke(level, req.location.getX(), req.location.getY(), req.location.getZ(), null);
                setDeltaMovement.invoke(tnt, req.velocity.getX(), req.velocity.getY(), req.velocity.getZ());
                setFuse.invoke(tnt, req.fuseTicks);
                
                // Set yield via NMS if available
                if (setYield != null) {
                    setYield.invoke(tnt, req.yield);
                }
                
                Class<?> spawnReason = Class.forName("org.bukkit.event.entity.CreatureSpawnEvent$SpawnReason");
                boolean added = (boolean) addEntity.invoke(level, tnt, spawnReason.getField("CUSTOM").get(null));
                
                if (added) {
                    spawnedNmsEntities.add(tnt);
                    
                    // Mark world as having Nuke TNT for ground detection
                    if (req.isNuke && entityListener != null) {
                        entityListener.markNukeActive(req.location.getWorld());
                    }
                    
                    return;
                }
            } catch (Throwable e) {
                // Log NMS failure
                com.vprolabs.vunstable.util.ErrorHandler.getInstance().handle(e, 
                    "AsyncSpawnEngine.spawnTNT() [NMS]", 
                    "AsyncSpawnEngine", "spawnTNT", 156,
                    null, req.location, 
                    "NMS TNT spawn failed, falling back to Bukkit API");
                // Fall through to Bukkit
            }
        }
        
        // Bukkit fallback
        try {
            TNTPrimed tnt = req.location.getWorld().spawn(req.location, TNTPrimed.class, entity -> {
                entity.setVelocity(req.velocity);
                entity.setFuseTicks(req.fuseTicks);
                // CRITICAL: Set yield for block damage
                entity.setYield(req.yield);
                
                // Add metadata for identification
                if (req.isNuke) {
                    entity.setMetadata(EntityListener.META_NUKE_ROD, new FixedMetadataValue(plugin, true));
                    // Add sync delay metadata for synchronized explosions
                    if (req.syncDelay > 0) {
                        entity.setMetadata(EntityListener.META_NUKE_SYNC_DELAY, new FixedMetadataValue(plugin, req.syncDelay));
                        entity.setMetadata(EntityListener.META_NUKE_RING_INDEX, new FixedMetadataValue(plugin, req.ringIndex));
                    }
                } else if (req.isStab) {
                    entity.setMetadata(EntityListener.META_STAB_ROD, new FixedMetadataValue(plugin, true));
                }
            }, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
            
            if (tnt != null) {
                spawnedBukkitEntities.add(tnt);
                
                // Mark world as having Nuke TNT
                if (req.isNuke && entityListener != null) {
                    entityListener.markNukeActive(req.location.getWorld());
                }
                
                plugin.getLogger().fine("[vUnstable] Spawned " + (req.isNuke ? "Nuke" : "Stab") + 
                    " TNT at " + req.location.getBlockX() + "," + req.location.getBlockY() + "," + req.location.getBlockZ() + 
                    " with fuse " + req.fuseTicks + ", yield " + req.yield);
            }
        } catch (Exception e) {
            com.vprolabs.vunstable.util.ErrorHandler.getInstance().handle(e, 
                "AsyncSpawnEngine.spawnTNT() [Bukkit]", 
                "AsyncSpawnEngine", "spawnTNT", 202,
                null, req.location, 
                "Bukkit TNT spawn failed at " + req.location);
        }
    }
    
    /**
     * Queue a TNT spawn request.
     */
    public void queueSpawn(Location location, Vector velocity, int fuseTicks) {
        spawnQueue.offer(new SpawnRequest(location, velocity, fuseTicks, 4.0f, false, false));
    }
    
    /**
     * Queue a TNT spawn request with full parameters.
     */
    public void queueSpawn(Location location, Vector velocity, int fuseTicks, float yield, boolean isNuke, boolean isStab) {
        spawnQueue.offer(new SpawnRequest(location, velocity, fuseTicks, yield, isNuke, isStab));
    }
    
    /**
     * Queue multiple spawns for a nuke ring.
     */
    public void queueRingSpawns(double centerX, double centerZ, double spawnY, double radius, int count, 
                                double velocityY, int fuseTicks, org.bukkit.World world) {
        queueRingSpawns(centerX, centerZ, spawnY, radius, count, velocityY, fuseTicks, world, 0, 0);
    }
    
    /**
     * Queue multiple spawns for a nuke ring with synchronization delay.
     * @param ringIndex The ring index (0 = inner, 9 = outer)
     * @param syncDelay Ticks to delay explosion after ground hit (for synchronized detonation)
     */
    public void queueRingSpawns(double centerX, double centerZ, double spawnY, double radius, int count, 
                                double velocityY, int fuseTicks, org.bukkit.World world, int ringIndex, int syncDelay) {
        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i) / count;
            double x = centerX + (Math.cos(angle) * radius);
            double z = centerZ + (Math.sin(angle) * radius);
            Location loc = new Location(world, x, spawnY, z);
            // Nuke TNT: long fuse, marked as nuke, full yield, with sync info
            spawnQueue.offer(new SpawnRequest(loc, new Vector(0, velocityY, 0), fuseTicks, 4.0f, true, false, ringIndex, syncDelay));
        }
    }
    
    /**
     * Queue multiple spawns for a nuke ring and return UUIDs for tracking.
     * This is used for reliable NukeRod tracking.
     */
    public List<UUID> queueRingSpawnsTracked(double centerX, double centerZ, double spawnY, double radius, int count, 
                                             double velocityY, int fuseTicks, org.bukkit.World world, int ringIndex, int syncDelay) {
        List<UUID> spawnedIds = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i) / count;
            double x = centerX + (Math.cos(angle) * radius);
            double z = centerZ + (Math.sin(angle) * radius);
            Location loc = new Location(world, x, spawnY, z);
            
            // Spawn immediately and capture UUID
            try {
                TNTPrimed tnt = world.spawn(loc, TNTPrimed.class, entity -> {
                    entity.setVelocity(new Vector(0, velocityY, 0));
                    entity.setFuseTicks(fuseTicks);
                    entity.setYield(4.0f);
                    entity.setMetadata(EntityListener.META_NUKE_ROD, new FixedMetadataValue(plugin, true));
                    entity.setMetadata(EntityListener.META_NUKE_RING_INDEX, new FixedMetadataValue(plugin, ringIndex));
                    if (syncDelay > 0) {
                        entity.setMetadata(EntityListener.META_NUKE_SYNC_DELAY, new FixedMetadataValue(plugin, syncDelay));
                    }
                }, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
                
                if (tnt != null) {
                    spawnedIds.add(tnt.getUniqueId());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[vUnstable] Failed to spawn tracked TNT: " + e.getMessage());
            }
        }
        
        return spawnedIds;
    }
    
    /**
     * Queue multiple spawns for a stab column.
     */
    public void queueColumnSpawns(double centerX, double centerZ, int startY, int endY,
                                   double velocityY, int fuseTicks, org.bukkit.World world) {
        for (int y = startY; y >= endY; y--) {
            Location loc = new Location(world, centerX, y, centerZ);
            // Stab TNT: short fuse, marked as stab, full yield
            queueSpawn(loc, new Vector(0, velocityY, 0), fuseTicks, 4.0f, false, true);
        }
    }
    
    /**
     * Queue an instant spawn (for INSTANT mode - zero velocity, no falling).
     */
    public void queueInstantSpawn(Location location, Vector velocity, int fuseTicks, 
                                   float yield, boolean isNuke, boolean isStab) {
        // For instant spawns, we process immediately rather than queuing
        // This ensures instant appearance at the target location
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                TNTPrimed tnt = location.getWorld().spawn(location, TNTPrimed.class, entity -> {
                    entity.setVelocity(velocity);  // Usually (0,0,0) for instant
                    entity.setFuseTicks(fuseTicks);
                    entity.setYield(yield);
                    entity.setGravity(false);  // Disable gravity for instant mode
                    
                    if (isNuke) {
                        entity.setMetadata(EntityListener.META_NUKE_ROD, new FixedMetadataValue(plugin, true));
                    } else if (isStab) {
                        entity.setMetadata(EntityListener.META_STAB_ROD, new FixedMetadataValue(plugin, true));
                    }
                }, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
                
                if (tnt != null) {
                    plugin.getLogger().fine("[vUnstable] Instant spawn " + (isStab ? "Stab" : "Nuke") + 
                        " TNT at " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[vUnstable] Failed to instant spawn TNT: " + e.getMessage());
            }
        });
    }
    
    public List<Object> getSpawnedNmsEntities() {
        return new ArrayList<>(spawnedNmsEntities);
    }
    
    public List<TNTPrimed> getSpawnedBukkitEntities() {
        return new ArrayList<>(spawnedBukkitEntities);
    }
    
    public void clearSpawnedEntities() {
        spawnedNmsEntities.clear();
        spawnedBukkitEntities.clear();
    }
    
    public void shutdown() {
        if (spawnTask != null) {
            spawnTask.cancel();
        }
        spawnQueue.clear();
    }
    
    public boolean isNmsAvailable() {
        return nmsAvailable;
    }
    
    /**
     * Internal class for spawn requests.
     */
    private static class SpawnRequest {
        final Location location;
        final Vector velocity;
        final int fuseTicks;
        final float yield;
        final boolean isNuke;
        final boolean isStab;
        final int ringIndex;
        final int syncDelay;
        
        SpawnRequest(Location location, Vector velocity, int fuseTicks, float yield, boolean isNuke, boolean isStab) {
            this(location, velocity, fuseTicks, yield, isNuke, isStab, 0, 0);
        }
        
        SpawnRequest(Location location, Vector velocity, int fuseTicks, float yield, boolean isNuke, boolean isStab, int ringIndex, int syncDelay) {
            this.location = location;
            this.velocity = velocity;
            this.fuseTicks = fuseTicks;
            this.yield = yield;
            this.isNuke = isNuke;
            this.isStab = isStab;
            this.ringIndex = ringIndex;
            this.syncDelay = syncDelay;
        }
    }
}
