package com.vprolabs.vunstable.listener;

import com.vprolabs.vunstable.rod.RodManager;
import com.vprolabs.vunstable.vUnstable;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * PlayerListener - Handles rod usage via right-click.
 * 
 * - Raycast to find target location
 * - Break rod on use
 * - Activate appropriate rod type
 */
public class PlayerListener implements Listener {
    
    private final JavaPlugin plugin;
    private final RodManager rodManager;
    private static final double RAYCAST_RANGE = 100.0;
    
    public PlayerListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rodManager = ((vUnstable) plugin).getRodManager();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        
        String rodType = rodManager.getRodType(item);
        if (rodType == null) return;
        
        event.setCancelled(true);
        
        // Raycast for target
        RayTraceResult result = raycast(player);
        if (result == null) return;
        
        Location target = result.getHitPosition().toLocation(player.getWorld());
        
        // Break rod
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItemInMainHand(item);
        
        // Play break sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        
        // Activate rod
        rodManager.activateRod(rodType, target, player);
    }
    
    private RayTraceResult raycast(Player player) {
        Vector start = player.getEyeLocation().toVector();
        Vector direction = player.getEyeLocation().getDirection();
        
        return player.getWorld().rayTraceBlocks(
            start.toLocation(player.getWorld()),
            direction,
            RAYCAST_RANGE,
            FluidCollisionMode.NEVER
        );
    }
}
