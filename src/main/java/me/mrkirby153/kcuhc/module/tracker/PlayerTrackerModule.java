package me.mrkirby153.kcuhc.module.tracker;

import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.team.LoneWolfTeam;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.TeamSpectator;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTime;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerTrackerModule extends UHCModule {

    private TeamHandler teamHandler;

    private Map<Player, Player> targets = new WeakHashMap<>();


    public PlayerTrackerModule(TeamHandler teamHandler) {
        super(Material.EYE_OF_ENDER, 0, "Compasses track players", true, "Compasses will point towards the closest player");
        this.teamHandler = teamHandler;
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("Compasses no longer track players!"));
        targets.clear();
        Bukkit.getOnlinePlayers().forEach(p -> {
            for(int i = 0; i < p.getInventory().getSize(); i++){
                ItemStack s = p.getInventory().getItem(i);
                if(s == null)
                    continue;
                if(s.getType() == Material.COMPASS){
                    ItemMeta m = s.getItemMeta();
                    if(m.getDisplayName() != null)
                        m.setDisplayName(null);
                    s.setItemMeta(m);
                    p.getInventory().setItem(i, s);
                    p.updateInventory();
                }
            }
        });
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("Compasses now track players!"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == UHCArena.State.ENDGAME) {
            // Clear compass targets
            targets.keySet().forEach(p -> {
                if (p.getBedSpawnLocation() != null)
                    p.setCompassTarget(p.getBedSpawnLocation());
                else
                    p.setCompassTarget(p.getWorld().getSpawnLocation());
            });
            targets.clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Remove the tracker target if they die
        Iterator<Map.Entry<Player, Player>> targetIterator = targets.entrySet().iterator();
        while (targetIterator.hasNext()) {
            Map.Entry<Player, Player> entry = targetIterator.next();
            if (entry.getValue().getUniqueId().equals(event.getEntity().getUniqueId())) {
                entry.getKey().sendMessage(UtilChat.message("The player you were tracking has died!"));
                targetIterator.remove();
            }
        }
        updateCompasses();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR)
            return;
        ItemStack item = event.getItem();
        if(item == null)
            return;
        if (item.getType() == Material.COMPASS) {
            HashSet<UUID> toExclude = new HashSet<>();
            Player player = event.getPlayer();
            UHCTeam currentTeam = teamHandler.getTeamForPlayer(player);
            if (currentTeam != null) {
                if (currentTeam instanceof TeamSpectator || currentTeam instanceof LoneWolfTeam)
                    return;
                // Exclude all players on your current team
                toExclude.addAll(currentTeam.getPlayers());
            }
            // Exclude spectators
            toExclude.addAll(teamHandler.spectatorsTeam().getPlayers());
            Player closestPlayer = findClosestPlayer(player.getLocation(), toExclude);
            if (closestPlayer == null) {
                player.sendMessage(UtilChat.message("Could not find a player"));
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 2F);
                updateCompasses();
            } else {
                double distance = UtilTime.trim(1, player.getLocation().distance(closestPlayer.getLocation()));
                player.sendMessage(UtilChat.message("Located " + ChatColor.BLUE + closestPlayer.getName() + " " + ChatColor.AQUA + distance + ChatColor.GRAY + " blocks away from you!"));
                targets.put(player, closestPlayer);
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 0.5F);
                updateCompasses();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove the player references from the map preventing memory leaks
        Iterator<Map.Entry<Player, Player>> iterator = targets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, Player> next = iterator.next();
            if (next.getKey().getUniqueId().equals(event.getPlayer().getUniqueId())) {
                iterator.remove();
                continue;
            }
            if (next.getValue().getUniqueId().equals(event.getPlayer().getUniqueId())) {
                next.getKey().sendMessage(UtilChat.message("The player you were tracking has disconnected"));
                iterator.remove();
            }
        }
        updateCompasses();
    }

    public void updateCompasses(){
        Bukkit.getOnlinePlayers().forEach(p -> updateCompasses(p, targets.get(p)));
    }

    @EventHandler(ignoreCancelled = true)
    public void updateCompass(UpdateEvent event) {
        if (event.getType() != UpdateType.TWO_SECOND)
            return;
        for(Map.Entry<Player, Player> e : targets.entrySet()){
            e.getKey().setCompassTarget(e.getValue().getLocation());
        }
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

    private Player findClosestPlayer(Location location, Collection<UUID> toIgnore) {
        double closestDistance = Double.MAX_VALUE;
        Player foundPlayer = null;
        for (Player p : Bukkit.getOnlinePlayers().stream()
                .filter(p -> !toIgnore.contains(p.getUniqueId())).filter(p-> p.getLocation().getWorld().equals(location.getWorld())).collect(Collectors.toList())) {
            double distance = location.distanceSquared(p.getLocation());
            if (distance < closestDistance) {
                foundPlayer = p;
                closestDistance = distance;
            }
        }
        return foundPlayer;
    }

    private void updateCompasses(Player tracker, Player tracked) {
        PlayerInventory inventory = tracker.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null)
                continue;
            if (item.getType() == Material.COMPASS) {
                // Do compass updating
                ItemMeta meta = item.getItemMeta();
                String tracking = (tracked == null) ? "Right click to select target" : tracked.getName();
                meta.setDisplayName(ChatColor.AQUA + "Player Tracker (" + ChatColor.GREEN + ChatColor.BOLD + tracking + ChatColor.RESET + ChatColor.AQUA + ")");
                item.setItemMeta(meta);
                inventory.setItem(i, item);
                tracker.updateInventory();
            }
        }
    }
}
