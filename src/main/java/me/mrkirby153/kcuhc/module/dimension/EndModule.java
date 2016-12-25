package me.mrkirby153.kcuhc.module.dimension;

import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.utils.UtilChat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public class EndModule extends UHCModule {

    public EndModule() {
        super(Material.ENDER_PORTAL_FRAME, 0, "Disable End", false, "Disables access to the end");
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("The End has been disabled!"));
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("The End has been enabled!"));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityPortal(EntityPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            event.getPlayer().sendMessage(UtilChat.generateLegacyError("The end is disabled!"));
            event.setCancelled(true);
        }
    }
}
