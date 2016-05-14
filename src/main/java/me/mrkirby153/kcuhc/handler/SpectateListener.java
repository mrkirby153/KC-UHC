package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SpectateListener implements Listener {

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event){
        if(TeamHandler.isSpectator(event.getPlayer()))
            event.setCancelled(true);
    }
    @EventHandler
    public void onExpTarget(EntityTargetEvent event){
        if(event.getEntity() instanceof ExperienceOrb){
            if(event.getTarget() instanceof Player){
                if(TeamHandler.isSpectator(((Player) event.getTarget())))
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event){
        if(event.getDamager() instanceof Player){
            if(TeamHandler.isSpectator((Player) event.getDamager()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event){
        if(event.getEntity() instanceof Player){
            if(event.getCause() == EntityDamageEvent.DamageCause.VOID)
                return;
            if(TeamHandler.isSpectator((Player) event.getEntity()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void hungerChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (TeamHandler.getTeamForPlayer(player) == TeamHandler.getTeamByName(TeamHandler.SPECTATORS_TEAM))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {
        if (TeamHandler.getTeamForPlayer(event.getPlayer()) == TeamHandler.getTeamByName(TeamHandler.SPECTATORS_TEAM))
            event.setCancelled(true);
    }

    @EventHandler
    public void onLogin(final PlayerLoginEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Hide all spectators
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (TeamHandler.getTeamForPlayer(p) == TeamHandler.spectatorsTeam()) {
                        event.getPlayer().hidePlayer(p);
                    }
                }
            }
        }.runTaskLater(UHC.plugin, 5);
    }

    @EventHandler
    public void onRespawn(final PlayerRespawnEvent event) {
    }
}
