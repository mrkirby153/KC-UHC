package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;

public class GameListener implements Listener {

    @EventHandler
    public void death(PlayerDeathEvent event) {
        event.getEntity().setGlowing(false);
        TeamHandler.leaveTeam(event.getEntity());
        UHC.arena.handleDeathMessage(event.getEntity(), event.getDeathMessage());
        event.getEntity().getWorld().strikeLightningEffect(event.getEntity().getLocation());
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

    @EventHandler
    public void changeWorld(PlayerChangedWorldEvent event){
        if(!event.getFrom().getName().contains("nether")){
            return;
        }
        double bounds = UHC.arena.getWorld().getWorldBorder().getSize() / 2;
        System.out.println("Bounds: +/- "+bounds);
        Player player = event.getPlayer();
        if(Math.abs(player.getLocation().getBlockZ()) > bounds || Math.abs(player.getLocation().getBlockX()) > bounds){
            System.out.println("Player has spawned outside the worldborder! Fixing");
            // Move the player diagonally into the worldborder
            Location toTeleport = player.getLocation().clone();
            System.out.println(String.format("Old Location: %s - %.2f, %.2f, %.2f", toTeleport.getWorld().getName(), toTeleport.getX(), toTeleport.getY(), toTeleport.getZ()));
            int attempts = 0;
            while(Math.abs(toTeleport.getBlockX()) > bounds-2) {
                if(toTeleport.getX() < 0){
                    toTeleport.setX(toTeleport.getX() + 0.5);
                } else {
                    toTeleport.setX(toTeleport.getX() - 0.5);
                }
                attempts++;
                if(attempts > 15000){
                    attempts = 0;
                    System.out.println("GIVING UP ON X");
                    break; // Give up
                }
            }
            while(Math.abs(toTeleport.getBlockZ()) > bounds-2){
                if(toTeleport.getZ() < 0){
                    toTeleport.setZ(toTeleport.getZ() + 0.5);
                } else {
                    toTeleport.setZ(toTeleport.getZ() - 0.5);
                }
                attempts++;
                if(attempts > 15000){
                    System.out.println("GIVING UP ON Z");
                    break; // Give up
                }
            }
            toTeleport = event.getPlayer().getWorld().getHighestBlockAt(toTeleport).getLocation().add(0.5, 0.5, 0.5);
            System.out.println(String.format("New Location: %s - %.2f, %.2f, %.2f", toTeleport.getWorld().getName(), toTeleport.getX(), toTeleport.getY(), toTeleport.getZ()));
            event.getPlayer().teleport(toTeleport);
            event.getPlayer().sendMessage(ChatColor.GOLD+"You would've spawned outside of the world border, so we moved you back in");
        }
    }
}
