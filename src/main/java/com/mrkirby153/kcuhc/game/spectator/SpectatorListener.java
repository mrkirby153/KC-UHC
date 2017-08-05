package com.mrkirby153.kcuhc.game.spectator;

import com.mrkirby153.kcuhc.game.UHCGame;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;

/**
 * Disallow pretty much everything to spectators
 */
public class SpectatorListener implements Listener {

    private UHCGame game;

    public SpectatorListener(UHCGame game) {
        this.game = game;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (game.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (game.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER && event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            if (game.isSpectator((Player) event.getEntity()))
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.PLAYER) {
            if (game.isSpectator((Player) event.getDamager()))
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() != null)
            if (event.getTarget().getType() == EntityType.PLAYER) {
                if (game.isSpectator((Player) event.getTarget())) {
                    event.setCancelled(true);
                }
            }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntityType() == EntityType.PLAYER)
            if (game.isSpectator((Player) event.getEntity()))
                event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (game.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (game.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (game.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event) {
        if (game.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (game.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (event.getAttacker().getType() == EntityType.PLAYER)
            if (game.isSpectator((Player) event.getAttacker()))
                event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onVehicleEntityCollision(VehicleEntityCollisionEvent event) {
        if (event.getEntity().getType() == EntityType.PLAYER)
            if (game.isSpectator((Player) event.getEntity()))
                event.setCancelled(true);
    }
}
