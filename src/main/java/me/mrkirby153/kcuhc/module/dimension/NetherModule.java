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

public class NetherModule extends UHCModule {

    public NetherModule() {
        super(Material.NETHERRACK, 0, "Disable Nether", false, "Disables access to the nether");
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("The Nether has been disabled!"));
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("The Nether has been enabled!"));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityPortal(EntityPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
            event.getPlayer().sendMessage(UtilChat.generateLegacyError("The nether is disabled!"));
            event.setCancelled(true);
        }
    }
}
