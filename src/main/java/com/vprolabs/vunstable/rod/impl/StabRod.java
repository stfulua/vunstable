package com.vprolabs.vunstable.rod.impl;

import com.vprolabs.vunstable.config.ConfigManager;
import com.vprolabs.vunstable.engine.AsyncSpawnEngine;
import com.vprolabs.vunstable.rod.RodManager;
import com.vprolabs.vunstable.vUnstable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * StabRod - The Bedrock Digger.
 * 
 * Creates a vertical shaft from surface to depth.
 * Two modes:
 * - INSTANT: TNT spawns directly at target depth (0.5s fuse)
 * - FALL: Traditional falling TNT from sky
 */
public class StabRod implements RodManager.Rod {
    
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final AsyncSpawnEngine spawnEngine;
    
    // Stab TNT configuration
    private static final float STAB_YIELD = 4.0f;  // Full explosion power
    private static final double STAB_VELOCITY = -10.0;
    
    public StabRod(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = ConfigManager.getInstance();
        this.spawnEngine = ((vUnstable) plugin).getSpawnEngine();
    }
    
    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text("Stab").color(TextColor.color(0x8B0000)));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            item.setItemMeta(meta);
        }
        
        item.setDurability((short) 63);
        return item;
    }
    
    @Override
    public void activate(Location target, Player player) {
        int startY = 0;
        int endY = 0;
        int count = 0;
        
        try {
            World world = target.getWorld();
            int surfaceY = world.getHighestBlockYAt(target.getBlockX(), target.getBlockZ());
            startY = surfaceY + 100;
            endY = Math.max(-64, target.getBlockY() - config.getStabDepth());
            
            // Get configurable fuse and spawn mode
            int fuseTicks = config.getStabFuseTicks();
            String spawnMode = config.getStabSpawnMode();
            boolean teleportEffect = config.isStabTeleportEffect();
            
            if ("INSTANT".equalsIgnoreCase(spawnMode)) {
                // INSTANT MODE: Spawn TNT directly at bottom
                activateInstantMode(target, player, world, surfaceY, endY, fuseTicks, teleportEffect);
            } else {
                // FALL MODE: Traditional falling TNT
                activateFallMode(target, player, world, startY, endY, fuseTicks);
            }
                
        } catch (Exception e) {
            com.vprolabs.vunstable.util.ErrorHandler.getInstance().handle(e, 
                "StabRod.activate()", 
                "StabRod", "activate", 58,
                player, target, 
                count + " TNT Stab spawn attempt from Y" + startY + " to Y" + endY);
            
            if (player != null && player.isOnline()) {
                player.sendMessage(net.kyori.adventure.text.Component.text(
                    "[vUnstable] An error occurred while activating the Stab rod! Staff have been notified.")
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        }
    }
    
    /**
     * INSTANT MODE: Spawn TNT directly at target depth with no falling physics.
     */
    private void activateInstantMode(Location target, Player player, World world, 
                                     int surfaceY, int endY, int fuseTicks, boolean teleportEffect) {
        int count = surfaceY - endY + 1;
        double centerX = target.getX();
        double centerZ = target.getZ();
        
        plugin.getLogger().info("[vUnstable] STAB INSTANT at X" + centerX + " Z" + centerZ + 
            " | Surface Y" + surfaceY + " to Bottom Y" + endY + " | " + count + " TNT");
        
        // Visual effect at surface (optional teleport animation)
        if (teleportEffect && player != null) {
            world.spawnParticle(Particle.PORTAL, target.getX(), surfaceY + 1, target.getZ(), 
                50, 0.5, 1, 0.5, 0.1);
            world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }
        
        // Small delay for visual effect, then spawn TNT at depth
        new BukkitRunnable() {
            @Override
            public void run() {
                int spawned = 0;
                for (int y = surfaceY; y >= endY; y--) {
                    // Slight random offset for natural look
                    double offsetX = (Math.random() - 0.5) * 0.3;
                    double offsetZ = (Math.random() - 0.5) * 0.3;
                    Location spawnLoc = new Location(world, centerX + offsetX, y, centerZ + offsetZ);
                    
                    // Spawn TNT with zero velocity (no fall)
                    spawnEngine.queueInstantSpawn(spawnLoc, new Vector(0, 0, 0), 
                        fuseTicks, 4.0f, false, true);
                    spawned++;
                }
                
                plugin.getLogger().info("[vUnstable] STAB INSTANT: " + spawned + " TNT teleported to depth, " +
                    "exploding in " + (fuseTicks / 20.0) + " seconds");
            }
        }.runTaskLater(plugin, teleportEffect ? 2L : 0L); // 0.1s delay if effect enabled
    }
    
    /**
     * FALL MODE: Traditional falling TNT from sky.
     */
    private void activateFallMode(Location target, Player player, World world, 
                                  int startY, int endY, int fuseTicks) {
        int count = startY - endY + 1;
        
        plugin.getLogger().info("[vUnstable] STAB FALL at X" + target.getX() + " Z" + target.getZ() + 
            " | Y" + startY + " to Y" + endY + " | " + count + " TNT");
        
        // Queue all TNT spawns for the column (traditional falling)
        spawnEngine.queueColumnSpawns(
            target.getX(), 
            target.getZ(), 
            startY, 
            endY,
            STAB_VELOCITY, 
            fuseTicks, 
            world
        );
        
        plugin.getLogger().info("[vUnstable] STAB FALL queued: " + count + " TNT with fuse " + fuseTicks + 
            " ticks, yield " + STAB_YIELD);
    }
    
    @Override
    public String getName() {
        return "Stab";
    }
}
