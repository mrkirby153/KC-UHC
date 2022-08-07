package com.mrkirby153.kcuhc.game.spectator;

import com.mrkirby153.kcuhc.game.UHCGame;
import me.mrkirby153.kcutils.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.entity.EntityType.PLAYER;

/**
 * Disallow pretty much everything to spectators
 */
public class SpectatorListener implements Listener {

    public static final List<String> COMMAND_WHITELIST = new ArrayList<>();

    static {
        COMMAND_WHITELIST.add("spectate");
    }

    private UHCGame game;

    public SpectatorListener(UHCGame game) {
        this.game = game;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (game.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (game.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() == PLAYER
            && event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            if (game.isSpectator((Player) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == PLAYER) {
            if (game.isSpectator((Player) event.getDamager())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() != null) {
            if (event.getTarget().getType() == PLAYER) {
                if (game.isSpectator((Player) event.getTarget())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntityType() == PLAYER) {
            if (game.isSpectator((Player) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (game.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (game.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (game.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event) {
        if (game.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (event.getEntityType() == PLAYER && game.isSpectator((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (event.getAttacker() != null) {
            if (event.getAttacker().getType() == PLAYER) {
                if (game.isSpectator((Player) event.getAttacker())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onVehicleEntityCollision(VehicleEntityCollisionEvent event) {
        if (event.getEntity().getType() == PLAYER) {
            if (game.isSpectator((Player) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!event.getMessage().startsWith("/")) {
            return; // This isn't a command
        }
        if (game.isSpectator(event.getPlayer()) && (!event.getPlayer().isOp() && !event.getPlayer()
            .hasPermission("kcuhc.spectate.command.bypass"))) {
            String commandName = event.getMessage().substring(1);
            if (commandName.contains(" ")) {
                commandName = commandName.split(" ")[0];
            }
            if (!COMMAND_WHITELIST.contains(commandName.toLowerCase())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(
                    Chat.message("Game", "You cannot use that command as a spectator")
                        .toLegacyText());
            }
        }
    }
}
