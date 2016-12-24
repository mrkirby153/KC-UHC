package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.Module;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public class DimensionHandler extends Module<UHC> implements Listener {

    public DimensionHandler(UHC plugin) {
        super("Dimension Handler", "1.0", plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityCreatePortal(EntityCreatePortalEvent event) {
        switch (event.getPortalType()) {
            case NETHER:
                if (!getPlugin().arena.getProperties().NETHER_ENABLED.get()) {
                    event.setCancelled(true);
                }
                break;
            case ENDER:
                if (!getPlugin().arena.getProperties().END_ENABLED.get()) {
                    event.setCancelled(true);
                }
                break;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityPortal(EntityPortalEvent event) {
        switch (event.getTo().getWorld().getEnvironment()) {
            case NETHER:
                if (!getPlugin().arena.getProperties().NETHER_ENABLED.get()) {
                    event.setCancelled(true);
                }
                break;
            case THE_END:
                if (!getPlugin().arena.getProperties().END_ENABLED.get()) {
                    event.setCancelled(true);
                }
                break;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        switch (event.getTo().getWorld().getEnvironment()) {
            case NETHER:
                if (!getPlugin().arena.getProperties().NETHER_ENABLED.get()) {
                    event.getPlayer().sendMessage(UtilChat.generateLegacyError("The nether is disabled!"));
                    event.setCancelled(true);
                }
                break;
            case THE_END:
                if (!getPlugin().arena.getProperties().END_ENABLED.get()) {
                    event.getPlayer().sendMessage(UtilChat.generateLegacyError("The end is disabled!"));
                    event.setCancelled(true);
                }
                break;
        }
    }

    @Override
    protected void init() {
        registerListener(this);
    }
}
