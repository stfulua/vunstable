package com.vprolabs.vunstable.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BukkitSchedulerManager - Standard Bukkit implementation of TaskScheduler.
 * Optimized for: Bukkit, Spigot, Paper, Purpur
 * Uses: BukkitScheduler (single global thread)
 */
public class BukkitSchedulerManager implements TaskScheduler {

    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> entityTasks = new ConcurrentHashMap<>();

    public BukkitSchedulerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPlatformName() {
        return "Bukkit";
    }

    @Override
    public boolean isPrimaryThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public void runTask(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void runTaskAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void runTaskLater(Runnable runnable, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    @Override
    public void runTaskTimer(Runnable runnable, long delayTicks, long periodTicks) {
        Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
    }

    @Override
    public void runAtLocation(Location location, Runnable runnable) {
        // Bukkit is global, so just run it
        runTask(runnable);
    }

    @Override
    public void runAtLocationDelayed(Location location, Runnable runnable, long delayTicks) {
        // Bukkit is global, so just run it delayed
        runTaskLater(runnable, delayTicks);
    }

    @Override
    public void runAtEntity(Entity entity, Runnable runnable) {
        // Bukkit is global, so just run it
        runTask(runnable);
    }

    @Override
    public ScheduledTask runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        // Bukkit: Use standard task timer (all runs on main thread anyway)
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if entity is still valid
                if (entity == null || entity.isDead()) {
                    this.cancel();
                    entityTasks.remove(entity.getUniqueId());
                    return;
                }
                task.run();
            }
        };
        
        BukkitTask bukkitTask = runnable.runTaskTimer(plugin, delayTicks, periodTicks);
        entityTasks.put(entity.getUniqueId(), bukkitTask);
        
        return new ScheduledTask() {
            @Override
            public void cancel() {
                bukkitTask.cancel();
                entityTasks.remove(entity.getUniqueId());
            }
            
            @Override
            public boolean isCancelled() {
                return bukkitTask.isCancelled();
            }
        };
    }

    @Override
    public void cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin);
        entityTasks.clear();
    }
}
