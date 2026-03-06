package com.vprolabs.vunstable.rod;

import com.vprolabs.vunstable.rod.impl.NukeRod;
import com.vprolabs.vunstable.rod.impl.StabRod;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * RodManager - Factory and registry for destruction rods.
 * 
 * Handles rod creation, identification, and activation.
 */
public class RodManager {
    
    private final JavaPlugin plugin;
    private final NamespacedKey rodTypeKey;
    private final Map<String, Rod> rodRegistry;
    
    public RodManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rodTypeKey = new NamespacedKey(plugin, "rod_type");
        this.rodRegistry = new HashMap<>();
        
        // Register rods
        registerRod("nuke", new NukeRod(plugin));
        registerRod("stab", new StabRod(plugin));
    }
    
    private void registerRod(String id, Rod rod) {
        rodRegistry.put(id.toLowerCase(), rod);
    }
    
    /**
     * Create a rod item stack.
     */
    public ItemStack createRod(String type) {
        Rod rod = rodRegistry.get(type.toLowerCase());
        if (rod == null) return null;
        
        ItemStack item = rod.createItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(rodTypeKey, PersistentDataType.STRING, type.toLowerCase());
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Get rod type from item.
     */
    public String getRodType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(rodTypeKey, PersistentDataType.STRING);
    }
    
    /**
     * Activate a rod at a location.
     */
    public boolean activateRod(String type, org.bukkit.Location target, org.bukkit.entity.Player player) {
        Rod rod = rodRegistry.get(type.toLowerCase());
        if (rod == null) return false;
        
        plugin.getLogger().info("[vUnstable] " + player.getName() + " activated " + type + " at " + 
            target.getBlockX() + "," + target.getBlockY() + "," + target.getBlockZ());
        
        rod.activate(target, player);
        return true;
    }
    
    /**
     * Rod interface.
     */
    public interface Rod {
        ItemStack createItem();
        void activate(org.bukkit.Location target, org.bukkit.entity.Player player);
        String getName();
    }
}
