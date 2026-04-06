package com.vprolabs.vunstable.scheduler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * TaskScheduler - Abstraction layer for scheduling tasks on both Bukkit/Paper and Folia.
 * 
 * Platform Support:
 * - Bukkit/Spigot/Paper/Purpur: Uses standard BukkitScheduler
 * - Folia: Uses RegionScheduler, EntityScheduler, GlobalRegionScheduler
 * 
 * Thread Safety:
 * - Always use runAtLocation() for block/world operations at a specific location
 * - Always use runAtEntity() for entity operations
 * - Use runTask() for global operations that don't touch specific entities/blocks
 */
public interface TaskScheduler {
    
    /**
     * Get the platform name this scheduler is optimized for.
     * @return "Bukkit" for Bukkit/Spigot/Paper/Purpur, "Folia" for Folia
     */
    String getPlatformName();
    
    /**
     * Check if current thread is the primary/main thread.
     * For Folia, this returns true if on the global region thread.
     */
    boolean isPrimaryThread();

    /**
     * Run a task on the next tick (global/main thread).
     */
    void runTask(Runnable runnable);

    /**
     * Run a task asynchronously.
     */
    void runTaskAsync(Runnable runnable);

    /**
     * Run a task after a delay.
     */
    void runTaskLater(Runnable runnable, long delayTicks);

    /**
     * Run a task repeatedly.
     */
    void runTaskTimer(Runnable runnable, long delayTicks, long periodTicks);

    /**
     * Run a task at a specific location (region-aware for Folia).
     */
    void runAtLocation(Location location, Runnable runnable);

    /**
     * Run a task after a delay at a specific location (region-aware for Folia).
     */
    void runAtLocationDelayed(Location location, Runnable runnable, long delayTicks);

    /**
     * Run a task for a specific entity (region-aware for Folia).
     */
    void runAtEntity(Entity entity, Runnable runnable);

    /**
     * Run a repeating task for a specific entity (region-aware for Folia).
     * The task will run on the entity's region thread.
     * 
     * @param entity The entity to run the task for
     * @param task The task to run (should check conditions and cancel when done)
     * @param delayTicks Ticks to wait before first execution
     * @param periodTicks Ticks between executions
     * @return A ScheduledTask that can be cancelled
     */
    ScheduledTask runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks);

    /**
     * Cancel all tasks.
     */
    void cancelAll();
}
