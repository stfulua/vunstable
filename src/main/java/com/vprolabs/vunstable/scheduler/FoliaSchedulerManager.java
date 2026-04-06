package com.vprolabs.vunstable.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * FoliaSchedulerManager - Implementation of TaskScheduler for Folia.
 * Optimized for: Folia (regionized threading)
 * Uses: GlobalRegionScheduler, RegionScheduler, EntityScheduler, AsyncScheduler
 */
public class FoliaSchedulerManager implements TaskScheduler {

    private final JavaPlugin plugin;

    public FoliaSchedulerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPlatformName() {
        return "Folia";
    }

    @Override
    public boolean isPrimaryThread() {
        // Folia has no single "primary thread" - check if on any server thread
        return Bukkit.isPrimaryThread();
    }

    @Override
    public void runTask(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> runnable.run());
    }

    @Override
    public void runTaskAsync(Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
    }

    @Override
    public void runTaskLater(Runnable runnable, long delayTicks) {
        if (delayTicks <= 0) {
            runTask(runnable);
            return;
        }
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delayTicks);
    }

    @Override
    public void runTaskTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (delayTicks <= 0) delayTicks = 1;
        if (periodTicks <= 0) periodTicks = 1;
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delayTicks, periodTicks);
    }

    @Override
    public void runAtLocation(Location location, Runnable runnable) {
        Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> runnable.run());
    }

    @Override
    public void runAtLocationDelayed(Location location, Runnable runnable, long delayTicks) {
        if (delayTicks <= 0) {
            runAtLocation(location, runnable);
            return;
        }
        Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduledTask -> runnable.run(), delayTicks);
    }

    @Override
    public void runAtEntity(Entity entity, Runnable runnable) {
        entity.getScheduler().run(plugin, scheduledTask -> runnable.run(), null);
    }

    @Override
    public ScheduledTask runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        if (delayTicks <= 0) delayTicks = 1;
        if (periodTicks <= 0) periodTicks = 1;
        
        final io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = 
            entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), null, delayTicks, periodTicks);
        
        return new ScheduledTask() {
            @Override
            public void cancel() {
                foliaTask.cancel();
            }
            
            @Override
            public boolean isCancelled() {
                return foliaTask.isCancelled();
            }
        };
    }

    @Override
    public void cancelAll() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }
}
