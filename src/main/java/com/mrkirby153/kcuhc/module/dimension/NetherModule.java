package com.mrkirby153.kcuhc.module.dimension;

import com.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public class NetherModule extends UHCModule {

    public NetherModule() {
        super("Disable Nether", "Disables the nether", Material.NETHER_BRICK);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(EntityPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
        }
    }
}
