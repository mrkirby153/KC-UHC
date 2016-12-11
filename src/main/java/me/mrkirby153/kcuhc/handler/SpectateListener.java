package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.TeamSpectator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;

public class SpectateListener implements Listener {

    private static HashSet<UUID> earlyPickup = new HashSet<>();
    
    private TeamHandler teamHandler;

    private UHC plugin;
    
    public SpectateListener(TeamHandler teamHandler, UHC plugin){
        this.teamHandler = teamHandler;
        this.plugin = plugin;
    }

    public static void addEarlyPickup(Player player) {
        earlyPickup.add(player.getUniqueId());
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (earlyPickup.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
        if (teamHandler.isSpectator(event.getPlayer())) {
            earlyPickup.remove(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            if (teamHandler.isSpectator((Player) event.getDamager()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if (teamHandler.isSpectator(player))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(HangingBreakEvent event){
        if(event instanceof HangingBreakByEntityEvent){
            Entity remover = ((HangingBreakByEntityEvent) event).getRemover();
            if(remover instanceof Player){
                if(teamHandler.isSpectator((Player) remover))
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event){
        if(teamHandler.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void vehicleDestroy(VehicleDestroyEvent event) {
        if (event.getAttacker() instanceof Player)
            if (teamHandler.isSpectator((Player) event.getAttacker()))
                event.setCancelled(true);
    }

    @EventHandler
    public void vehicleDamage(VehicleDamageEvent event) {
        if (event.getAttacker() instanceof Player) {
            if (teamHandler.isSpectator((Player) event.getAttacker()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void vehiclePush(VehicleEntityCollisionEvent event){
        if(event.getEntity() instanceof Player){
            if(teamHandler.isSpectator((Player) event.getEntity()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (teamHandler.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID)
                return;
            if (teamHandler.isSpectator((Player) event.getEntity())) {
                event.getEntity().setFireTicks(0);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void hungerChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (teamHandler.getTeamForPlayer(player) instanceof TeamSpectator)
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {
        if (teamHandler.getTeamForPlayer(event.getPlayer()) instanceof TeamSpectator)
            event.setCancelled(true);
    }

    @EventHandler
    public void onLogin(final PlayerLoginEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Hide all spectators
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (teamHandler.getTeamForPlayer(p) == teamHandler.spectatorsTeam()) {
                        event.getPlayer().hidePlayer(p);
                    }
                }
            }
        }.runTaskLater(plugin, 5);
    }

    @EventHandler
    public void onRespawn(final PlayerRespawnEvent event) {
        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> earlyPickup.remove(event.getPlayer().getUniqueId()), 10L);

    }
}
