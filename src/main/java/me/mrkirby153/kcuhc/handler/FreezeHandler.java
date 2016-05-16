package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

public class FreezeHandler implements Listener {

    private static ArrayList<UUID> frozenPlayers = new ArrayList<>();

    private static HashMap<UUID, ArrayList<PotionEffect>> potions = new HashMap<>();

    private static ArrayList<Location> placedBlocks = new ArrayList<>();

    public static boolean pvpEnabled = true;

    public static boolean isFrozen() {
        return UHC.arena != null && UHC.arena.currentState() == UHCArena.State.FROZEN;
    }

    public static void freezePlayer(Player player) {
        frozenPlayers.add(player.getUniqueId());
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You have been frozen!");
        player.sendMessage("   PVP has been disabled and you may no longer interact with the world!");
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1F, 1F);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, true));
        ArrayList<PotionEffect> effects = new ArrayList<>();
        effects.addAll(player.getActivePotionEffects());
        Iterator<PotionEffect> i = effects.iterator();
        while(i.hasNext()){
            if(i.next().getType() == PotionEffectType.BLINDNESS){
                System.out.println("removing blindness");
                i.remove();
            }
        }
        potions.put(player.getUniqueId(), effects);
    }

    public static void unfreeze(Player player) {
        frozenPlayers.remove(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "You have been unfrozen!");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1F, 1F);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        ArrayList<PotionEffect> effects = potions.remove(player.getUniqueId());
        if (effects != null) {
            for (PotionEffect e : effects) {
                if (player.hasPotionEffect(e.getType()))
                    player.removePotionEffect(e.getType());
                player.addPotionEffect(e);
            }
        }
        player.setOp(false);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    public static void freezebypass(Player player) {
        frozenPlayers.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.setOp(true);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage(ChatColor.GOLD + "Unfrozen!");
    }

    public static void restoreBlocks() {
        for (Location l : placedBlocks) {
            l.getBlock().setType(Material.AIR);
        }
        placedBlocks = new ArrayList<>();
        Bukkit.broadcastMessage(ChatColor.GOLD + "Damage has been re-enabled!");
    }

    public FreezeHandler(UHC plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void moveEvent(PlayerMoveEvent event) {
        if (!isFrozen())
            return;
        if (!frozenPlayers.contains(event.getPlayer().getUniqueId()))
            return;
        Block relative = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
        Material below = relative.getType();
        if (below == Material.AIR || below == Material.LAVA || below == Material.STATIONARY_LAVA || below == Material.WATER || below == Material.STATIONARY_WATER) {
            placedBlocks.add(relative.getLocation());
            relative.setType(Material.BARRIER);
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to.getX() != from.getX() || to.getZ() != from.getZ())
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        if (!isFrozen())
            return;
        if (!frozenPlayers.contains(event.getPlayer().getUniqueId()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void bow(EntityShootBowEvent event) {
        if (!isFrozen())
            return;
        if (frozenPlayers.contains(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void blockPlace(BlockPlaceEvent event) {
        if (!isFrozen())
            return;
        if (frozenPlayers.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void bucketFill(PlayerBucketFillEvent event) {
        if (!isFrozen())
            return;
        if (frozenPlayers.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void bucketEmpty(PlayerBucketEmptyEvent event) {
        if (!isFrozen())
            return;
        if (frozenPlayers.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityDamageEvent(EntityDamageEvent event) {
        if (!pvpEnabled)
            event.setCancelled(true);
        if (!isFrozen())
            return;
        if (frozenPlayers.contains(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void consume(PlayerItemConsumeEvent event) {
        if (!isFrozen())
            return;
        if (!frozenPlayers.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void saturation(FoodLevelChangeEvent event) {
        if (!isFrozen())
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void itemDropEvent(PlayerDropItemEvent event) {
        if (!isFrozen())
            return;
        if (frozenPlayers.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

}
