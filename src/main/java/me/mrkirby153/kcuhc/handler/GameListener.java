package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffectType;

public class GameListener implements Listener {

    @EventHandler
    public void death(PlayerDeathEvent event) {
        event.getEntity().setGlowing(false);
        TeamHandler.leaveTeam(event.getEntity());
        UHC.arena.handleDeathMessage(event.getEntity(), event.getDeathMessage());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UHC.arena.playerJoin(event.getPlayer());
        event.setJoinMessage("");
        if (TeamHandler.getTeamForPlayer(event.getPlayer()) == null)
            TeamHandler.joinTeam(TeamHandler.spectatorsTeam(), event.getPlayer());
        else
            TeamHandler.joinTeam(TeamHandler.getTeamForPlayer(event.getPlayer()), event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage("");
        UHC.arena.playerDisconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }
        System.out.println(event.getEntity().getName() + " was damaged by " + event.getCause().toString());
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || event.getCause() == EntityDamageEvent.DamageCause.MAGIC || event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            return;
        }
        double oldDamage = event.getDamage();
        double newDamage = Math.floor(oldDamage / 2);
        if(newDamage < 1)
            newDamage = 1;
        event.setDamage(newDamage);
        System.out.println(String.format("[DMG] OLD: [%.2f] NEW: [%.2f]", oldDamage, newDamage));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void entityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.PLAYER) {
            return;
        }
        double oldDamage = event.getDamage();
        double newDamage = Math.floor(oldDamage / 2);
        if(newDamage < 1)
            newDamage = 1;
        event.setDamage(newDamage);
        System.out.println(String.format("[DMG] OLD: [%.2f] NEW: [%.2f]", oldDamage, newDamage));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event){
        UHC.arena.spectate(event.getPlayer());
        Bukkit.getServer().getScheduler().runTaskLater(UHC.plugin, ()->{
            if(!event.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)){
                UHC.arena.spectate(event.getPlayer());
            }
        }, 10L);
    }
}
