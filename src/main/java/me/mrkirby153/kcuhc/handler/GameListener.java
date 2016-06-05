package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.Random;

public class GameListener implements Listener {

    private static final Random random = new Random();

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
        event.setJoinMessage(ChatColor.BLUE + "Join> " + ChatColor.GRAY + event.getPlayer().getName());
        if (TeamHandler.getTeamForPlayer(event.getPlayer()) == null)
            TeamHandler.joinTeam(TeamHandler.spectatorsTeam(), event.getPlayer());
        else
            TeamHandler.joinTeam(TeamHandler.getTeamForPlayer(event.getPlayer()), event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage(ChatColor.BLUE + "Part> " + ChatColor.GRAY + event.getPlayer().getName());
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
        if (newDamage < 1)
            newDamage = 1;
        event.setDamage(newDamage);
        System.out.println(String.format("[DMG] OLD: [%.2f] NEW: [%.2f]", oldDamage, newDamage));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void entityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.PLAYER || event.getEntity().getType() != EntityType.PLAYER) {
            return;
        }
        double oldDamage = event.getDamage();
        double newDamage = Math.floor(oldDamage / 2);
        if (newDamage < 1)
            newDamage = 1;
        event.setDamage(newDamage);
        System.out.println(String.format("[DMG] OLD: [%.2f] NEW: [%.2f]", oldDamage, newDamage));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getServer().getScheduler().runTaskLater(UHC.plugin, () -> UHC.arena.spectate(event.getPlayer()), 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityInteract(PlayerInteractEvent evt) {
        if (TeamHandler.isSpectator(evt.getPlayer()))
            return;
        if (evt.getAction() == Action.RIGHT_CLICK_AIR || evt.getAction() == Action.RIGHT_CLICK_BLOCK)
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!TeamHandler.isSpectator(p))
                    continue;
                Block clickedAgainst = evt.getClickedBlock();
                if (clickedAgainst == null)
                    return;
                BlockFace clickedFace = evt.getBlockFace();
                Block newBlock = clickedAgainst.getWorld().getBlockAt(clickedAgainst.getX() + clickedFace.getModX(), clickedAgainst.getY() + clickedFace.getModY(), clickedAgainst.getZ() + clickedFace.getModZ());
                // Check if there is a player at this block
                double dist = newBlock.getLocation().distanceSquared(p.getLocation());
                Location newLoc = p.getLocation().clone().add(0, 1, 0);
                double newDist = newBlock.getLocation().distanceSquared(newLoc);
                if (dist < 2 || newDist < 0.7225) {
                    Location loc = p.getLocation().clone();
                    double toAdd = 1.3;
                    if (newDist < 0.7225)
                        toAdd += 0.5;
                    loc.setY(loc.getY() + toAdd);
                    p.sendMessage(UtilChat.message("You are in the way of a player and have been moved"));
                    p.teleport(loc);
                }
            }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockPlace(BlockPlaceEvent evt) {
    }

    @EventHandler
    public void changeWorld(PlayerChangedWorldEvent event) {
        if (!event.getFrom().getName().contains("nether")) {
            return;
        }
        double bounds = UHC.arena.getWorld().getWorldBorder().getSize() / 2;
        System.out.println("Bounds: +/- " + bounds);
        Player player = event.getPlayer();
        if (Math.abs(player.getLocation().getBlockZ()) > bounds || Math.abs(player.getLocation().getBlockX()) > bounds) {
            System.out.println("Player has spawned outside the worldborder! Fixing");
            // Move the player diagonally into the worldborder
            Location toTeleport = player.getLocation().clone();
            System.out.println(String.format("Old Location: %s - %.2f, %.2f, %.2f", toTeleport.getWorld().getName(), toTeleport.getX(), toTeleport.getY(), toTeleport.getZ()));
            int attempts = 0;
            while (Math.abs(toTeleport.getBlockX()) > bounds - 2) {
                if (toTeleport.getX() < 0) {
                    toTeleport.setX(toTeleport.getX() + 0.5);
                } else {
                    toTeleport.setX(toTeleport.getX() - 0.5);
                }
                attempts++;
                if (attempts > 15000) {
                    attempts = 0;
                    System.out.println("GIVING UP ON X");
                    break; // Give up
                }
            }
            while (Math.abs(toTeleport.getBlockZ()) > bounds - 2) {
                if (toTeleport.getZ() < 0) {
                    toTeleport.setZ(toTeleport.getZ() + 0.5);
                } else {
                    toTeleport.setZ(toTeleport.getZ() - 0.5);
                }
                attempts++;
                if (attempts > 15000) {
                    System.out.println("GIVING UP ON Z");
                    break; // Give up
                }
            }
            toTeleport = event.getPlayer().getWorld().getHighestBlockAt(toTeleport).getLocation().add(0.5, 0.5, 0.5);
            System.out.println(String.format("New Location: %s - %.2f, %.2f, %.2f", toTeleport.getWorld().getName(), toTeleport.getX(), toTeleport.getY(), toTeleport.getZ()));
            event.getPlayer().teleport(toTeleport);
            event.getPlayer().sendMessage(UtilChat.message("You have been moved inside the world border"));
        }
    }


    @EventHandler
    public void spawnEvent(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL)
            return;
        if (UHC.arena.endSize() <= UHC.arena.getWorld().getWorldBorder().getSize()) {
            int num = random.nextInt(100);
            if (num < 75) {
                event.setCancelled(true);
            }
        }
    }
}
