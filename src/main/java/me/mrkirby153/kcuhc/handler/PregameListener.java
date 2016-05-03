package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.io.File;
import java.util.UUID;

public class PregameListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(ChatColor.GREEN + event.getPlayer().getName() + " has joined!");
        event.getPlayer().setAllowFlight(true);
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        // Remove the player's data
        if (UHC.arena.currentState() != UHCArena.State.RUNNING) {
            UUID u = event.getUniqueId();
            for (World w : Bukkit.getWorlds()) {
                File worldFolder = new File(Bukkit.getWorldContainer(), w.getName());
                File dataFolder = new File(worldFolder, "playerdata");
                File datFile = new File(dataFolder, u.toString() + ".dat");
                if (datFile.exists())
                    if (datFile.delete()) {
                        UHC.plugin.getLogger().info("Deleted " + event.getName() + "'s data for world " + w.getName());
                    } else {
                        UHC.plugin.getLogger().warning("Could not delete " + event.getName() + "'s data for world " + w.getName());
                    }
            }
        }
    }

    @EventHandler
    public void entityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getEntity().getFireTicks() > 0)
                event.getEntity().setFireTicks(0);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void itemPickup(PlayerPickupItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void itemDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void weatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }
}
