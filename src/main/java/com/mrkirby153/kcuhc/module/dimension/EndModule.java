package com.mrkirby153.kcuhc.module.dimension;

import com.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public class EndModule extends UHCModule {

    public EndModule() {
        super("Disable End", "Disables the end", Material.ENDER_EYE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(EntityPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            event.setCancelled(true);
        }
    }
}
