package com.vprolabs.vunstable.scheduler;

/**
 * ScheduledTask - Represents a scheduled task that can be cancelled.
 * Abstraction for both Bukkit and Folia scheduled tasks.
 */
public interface ScheduledTask {
    
    /**
     * Cancel this task.
     */
    void cancel();
    
    /**
     * Check if this task is cancelled.
     */
    boolean isCancelled();
}
