package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.arena.UHCArena;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class FreezeHandler implements Listener, Runnable {

    private static ArrayList<UUID> frozenPlayers = new ArrayList<>();

    private static HashMap<UUID, ArrayList<PotionEffect>> potions = new HashMap<>();

    private static ArrayList<Location> placedBlocks = new ArrayList<>();

    private static HashMap<UUID, ItemStack[]> savedInventories = new HashMap<>();

    private static ArrayList<UUID> bypassedPlayers = new ArrayList<>();

    private static HashMap<UUID, Vector> velocity = new HashMap<>();

    public static boolean pvpEnabled = true;

    public static HashMap<Entity, Location> frozenEntities = new HashMap<>();

    public static boolean isFrozen() {
        return UHC.arena != null && UHC.arena.currentState() == UHCArena.State.FROZEN;
    }

    public static void freezePlayer(Player player) {
        frozenPlayers.add(player.getUniqueId());
        Vector velocity = UHC.velocityTracker.getVelocity(player);
        FreezeHandler.velocity.put(player.getUniqueId(), velocity);
/*        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You have been frozen!");
        player.sendMessage("   PVP has been disabled and you may no longer interact with the world!");*/
        player.sendMessage(UtilChat.message("You are now frozen"));
        player.sendMessage(UtilChat.message("PVP is disabled and you cannot interact with the world"));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1F, 1F);
        ArrayList<PotionEffect> effects = new ArrayList<>();
        effects.addAll(player.getActivePotionEffects());
        for (PotionEffect e : player.getActivePotionEffects()) {
            player.removePotionEffect(e.getType());
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, true));
        Iterator<PotionEffect> i = effects.iterator();
        while (i.hasNext()) {
            if (i.next().getType() == PotionEffectType.BLINDNESS) {
                i.remove();
            }
        }
        potions.put(player.getUniqueId(), effects);
    }

    public static void unfreeze(Player player) {
        frozenPlayers.remove(player.getUniqueId());
        player.sendMessage(UtilChat.message("You are no longer frozen"));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1F, 1F);
        for (PotionEffect e : player.getActivePotionEffects()) {
            player.removePotionEffect(e.getType());
        }
        ArrayList<PotionEffect> effects = potions.remove(player.getUniqueId());
        if (effects != null) {
            for (PotionEffect e : effects) {
                if (e.getType() == PotionEffectType.BLINDNESS) {
                    continue;
                }
                if (player.hasPotionEffect(e.getType()))
                    player.removePotionEffect(e.getType());
                player.addPotionEffect(e);
            }
        }
        Vector vector = velocity.get(player.getUniqueId());
        if (vector != null)
            player.setVelocity(vector);
        bypassedPlayers.remove(player.getUniqueId());
        player.setOp(false);
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR)
            player.setAllowFlight(false);
        player.setFlying(false);
        restoreInventory(player);
    }

    public static void playerUnfreeze(Player player) {
        frozenPlayers.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    public static void freezebypass(Player player) {
        frozenPlayers.remove(player.getUniqueId());
        bypassedPlayers.add(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.setOp(true);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage(UtilChat.message("You have bypassed being frozen"));
        saveInventory(player);
    }

    public static void restoreBlocks() {
        for (Location l : placedBlocks) {
            l.getBlock().setType(Material.AIR);
        }
        placedBlocks = new ArrayList<>();
    }


    private static void saveInventory(Player player) {
        ItemStack[] items = new ItemStack[player.getInventory().getSize()];
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            items[i] = player.getInventory().getItem(i);
        }
        savedInventories.put(player.getUniqueId(), items);
        player.getInventory().clear();
        player.updateInventory();
    }

    private static void restoreInventory(Player player) {
        if (!savedInventories.containsKey(player.getUniqueId()))
            return;
        ItemStack[] items = savedInventories.remove(player.getUniqueId());
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            player.getInventory().setItem(i, items[i]);
        }
        player.updateInventory();
    }

    public FreezeHandler(UHC plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 3L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void moveEvent(PlayerMoveEvent event) {
        if (!isFrozen())
            return;
        if (!frozenPlayers.contains(event.getPlayer().getUniqueId()))
            return;
        Block relative = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
        Material below = relative.getType();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ())
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
    public void entityTargetEvent(EntityTargetLivingEntityEvent event) {
        if (!isFrozen()) {
            return;
        }
        if (frozenPlayers.contains(event.getTarget().getUniqueId())) {
            event.setCancelled(true);
        }
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

    @Override
    public void run() {
        for (Map.Entry<Entity, Location> e : frozenEntities.entrySet()) {
            Entity ent = e.getKey();
            PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, 20, 120, true, false);
            if (ent instanceof LivingEntity && !(ent instanceof Player)) {
                ((LivingEntity) ent).addPotionEffect(slowness);
            }
            if (e.getValue().distanceSquared(ent.getLocation()) > Math.pow(0.5, 2)) {
                ent.teleport(e.getValue());
            }
        }
    }
}
