package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilTime;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.TeamSpectator;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerTrackerHandler implements Listener, Runnable {

    private UHC plugin;

    private HashMap<UUID, UUID> targets = new HashMap<>();

    public PlayerTrackerHandler(UHC plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 5L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void run() {
        Iterator<Map.Entry<UUID, UUID>> iterator = targets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> next = iterator.next();
            UUID trackerUUID = next.getKey();
            UUID trackedUUID = next.getValue();

            Player tracker = Bukkit.getPlayer(trackerUUID);
            Player tracked = Bukkit.getPlayer(trackedUUID);

            if (tracked == null) {
                updateCompasses(tracker, null);
                tracker.setCompassTarget(tracker.getWorld().getSpawnLocation());
            }
            if (tracker == null || tracked == null) {
                iterator.remove();
                continue;
            }
            tracker.setCompassTarget(tracked.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        List<UUID> toExclude = new ArrayList<>();
        UHCTeam currentTeam = TeamHandler.getTeamForPlayer(event.getPlayer());
        if (currentTeam != null) {
            if(currentTeam instanceof TeamSpectator)
                return;
            toExclude.addAll(currentTeam.getPlayers());
        }
        toExclude.addAll(TeamHandler.spectatorsTeam().getPlayers());

        Player trackedPlayer = findClosestPlayer(event.getPlayer().getLocation(), toExclude);

        if (trackedPlayer != null) {
            double distance = UtilTime.trim(1, getDistance(trackedPlayer.getLocation(), event.getPlayer().getLocation()));
            event.getPlayer().sendMessage(ChatColor.BLUE + "> " + ChatColor.GRAY + "Located " + ChatColor.BLUE + trackedPlayer.getName() +
                    " " + ChatColor.AQUA + distance + ChatColor.GRAY + " blocks away from you");
            targets.put(event.getPlayer().getUniqueId(), trackedPlayer.getUniqueId());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 0.5F);
            updateCompasses(event.getPlayer(), trackedPlayer);
        } else {
            event.getPlayer().sendMessage(ChatColor.BLUE + "> " + ChatColor.WHITE + "Could not find a closest player!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();

        // Remove compasses from drops
        Iterator<ItemStack> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            if (drops.next().getType() == Material.COMPASS)
                drops.remove();
        }

        UUID uuid = dead.getUniqueId();
        Iterator<Map.Entry<UUID, UUID>> iterator = targets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            if (entry.getKey().equals(uuid)) {
                iterator.remove();
            }
            if (entry.getValue().equals(uuid)) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null) {
                    p.sendMessage(ChatColor.BLUE + "> " + ChatColor.WHITE + "The player you were tracking has died!");
                }
                p.setCompassTarget(p.getWorld().getSpawnLocation());
                updateCompasses(p, null);
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void playerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getItemMeta() == null)
            return;
        if (item.getItemMeta().getDisplayName() == null)
            return;
        if (!item.getItemMeta().getDisplayName().contains("Player Tracker")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void prepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe().getResult() == null)
            return;

        if (event.getInventory() == null)
            return;

        if (event.getRecipe().getResult().getType() != Material.COMPASS)
            return;
        ItemStack playerTracker = event.getRecipe().getResult();
        ItemMeta meta = playerTracker.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Player Tracker (" + ChatColor.GREEN +
                ChatColor.BOLD + "Right click to select target" + ChatColor.RESET + ChatColor.AQUA + ")");
        playerTracker.setItemMeta(meta);
        event.getInventory().setResult(playerTracker);
    }

    public double distanceToTarget(UUID tracker) {
        Player player = Bukkit.getPlayer(tracker);
        if (player == null)
            return Double.NEGATIVE_INFINITY;
        Player target = getTarget(tracker);
        if (target == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return UtilTime.trim(1, target.getLocation().distance(player.getLocation()));
    }

    public Player getTarget(UUID tracker) {
        return Bukkit.getPlayer(targets.get(tracker));
    }

    private Player findClosestPlayer(Location location, List<UUID> toExclude) {
        List<UUID> uuidsToUse = Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toList());
        uuidsToUse.removeAll(toExclude);
        List<Player> players = uuidsToUse.stream().map(Bukkit::getPlayer).filter(p -> p != null).collect(Collectors.toList());

        double distance = Integer.MAX_VALUE;
        Player foundPlayer = null;
        for (Player p : players) {
            double distSquared = location.distanceSquared(p.getLocation());
            if (distSquared < distance) {
                foundPlayer = p;
                distance = distSquared;
            }
        }
        return foundPlayer;
    }

    private double getDistance(Location first, Location second) {
        return first.distance(second);
    }

    private void updateCompasses(Player tracker, Player tracked) {
        for (int i = 0; i < tracker.getInventory().getSize(); i++) {
            ItemStack item = tracker.getInventory().getItem(i);
            if (item == null)
                continue;
            if (item.getType() == Material.COMPASS) {
                ItemMeta meta = item.getItemMeta();
                String tracking = (tracked == null) ? "Right Click to select target" : tracked.getName();
                meta.setDisplayName(ChatColor.AQUA + "Player Tracker (" + ChatColor.GREEN + ChatColor.BOLD + tracking + ChatColor.RESET + ChatColor.AQUA + ")");
                item.setItemMeta(meta);
                tracker.getInventory().setItem(i, item);
                tracker.updateInventory();
            }
        }
    }
}