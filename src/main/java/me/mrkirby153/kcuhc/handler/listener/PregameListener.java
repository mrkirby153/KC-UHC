package me.mrkirby153.kcuhc.handler.listener;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.io.File;
import java.util.UUID;

public class PregameListener implements Listener {

    private UHC plugin;

    public PregameListener(UHC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent event) {
        if (!isPregame())
            return;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void entityDamage(EntityDamageEvent event) {
        if (!isPregame())
            return;
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID)
            return;
        if (event.getEntity() instanceof Player) {
            if (event.getEntity().getFireTicks() > 0)
                event.getEntity().setFireTicks(0);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void entityInteract(PlayerInteractEntityEvent event) {
        if (!isPregame())
            return;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void entityTarget(EntityTargetEvent event) {
        if (!isPregame())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void hangingBreak(HangingBreakByEntityEvent event) {
        if (!isPregame())
            return;
        if (event.getRemover().getType() == EntityType.PLAYER) {
            if (((Player) event.getRemover()).getGameMode() == GameMode.CREATIVE)
                return;
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void itemDrop(PlayerDropItemEvent event) {
        if (!isPregame())
            return;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void itemPickup(PlayerPickupItemEvent event) {
        if (!isPregame())
            return;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isPregame())
            return;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!isPregame())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isPregame())
            return;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isPregame())
            return;
        event.setJoinMessage(ChatColor.GREEN + event.getPlayer().getName() + " has joined!");
        event.getPlayer().setAllowFlight(true);
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (!isPregame())
            return;
        // Remove the player's data
        if (plugin.arena.currentState() != UHCArena.State.RUNNING) {
            UUID u = event.getUniqueId();
            for (World w : Bukkit.getWorlds()) {
                File worldFolder = new File(Bukkit.getWorldContainer(), w.getName());
                File dataFolder = new File(worldFolder, "playerdata");
                File datFile = new File(dataFolder, u.toString() + ".dat");
                if (datFile.exists())
                    if (datFile.delete()) {
                        plugin.getLogger().info("Deleted " + event.getName() + "'s data for world " + w.getName());
                    } else {
                        plugin.getLogger().warning("Could not delete " + event.getName() + "'s data for world " + w.getName());
                    }
            }
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        if (!isPregame())
            return;
        if (plugin.arena.currentState() != UHCArena.State.RUNNING) {
            // Remove the player from the arena
            plugin.arena.removePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void vehicleDamage(VehicleDamageEvent event) {
        if (!isPregame())
            return;
        if (event.getAttacker() instanceof Player) {
            if (((Player) event.getAttacker()).getGameMode() == GameMode.CREATIVE) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void vehicleDestroy(VehicleDestroyEvent event) {
        if (!isPregame())
            return;
        if (event.getAttacker() instanceof Player) {
            if (((Player) event.getAttacker()).getGameMode() == GameMode.CREATIVE)
                return;
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void vehiclePush(VehicleEntityCollisionEvent event) {
        if (!isPregame())
            return;
        if (event.getEntity() instanceof Player) {
            if (((Player) event.getEntity()).getGameMode() == GameMode.CREATIVE)
                return;
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void weatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    private boolean isPregame() {
        return plugin.arena.currentState() == UHCArena.State.INITIALIZED || plugin.arena.currentState() == UHCArena.State.WAITING || plugin.arena.currentState() == UHCArena.State.ENDGAME;
    }
}
