package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.Time.TimeUnit;
import me.mrkirby153.kcutils.cooldown.Cooldown;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerTrackerModule extends UHCModule {

    private Map<Player, Player> targets = new HashMap<>();

    private UHC uhc;
    private UHCGame game;

    private Cooldown<UUID> cooldown = new Cooldown<>(10 * 1000, "Player Tracker", true);


    @Inject
    public PlayerTrackerModule(UHC uhc, UHCGame game) {
        super("Player Tracker", "Compasses will point towards the closest player",
            Material.ENDER_PEARL);
        this.uhc = uhc;
        this.game = game;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ENDING) {
            Bukkit.getOnlinePlayers().forEach(this::resetCompass);
            this.targets.clear();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (game.isSpectator(player)) {
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK
            || event.getAction() == Action.LEFT_CLICK_AIR) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (item.getType() == Material.COMPASS) {
            if (!cooldown.check(player.getUniqueId())) {
                player.spigot().sendMessage(Chat.INSTANCE
                    .message("Cooldown", "You can use this again in {time}", "{time}", Time.INSTANCE
                        .format(1, cooldown.getTimeLeft(player.getUniqueId()), TimeUnit.FIT)));
                return;
            } else {
                cooldown.use(player.getUniqueId());
            }
            HashSet<UUID> toExclude = new HashSet<>();
            UHCTeam team = (UHCTeam) game.getTeam(player);
            if (team != null) {
                toExclude.addAll(team.getPlayers());
            }
            toExclude.add(player.getUniqueId());
            toExclude.addAll(game.getSpectators().getPlayers());

            Player closestPlayer = findClosestPlayer(player.getLocation(), toExclude);
            if (closestPlayer == null) {
                player.spigot().sendMessage(Chat.INSTANCE.error("Could not find a player"));
                this.targets.remove(player);
                this.resetCompass(player);
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 2F);
            } else {
                double distance = Time.INSTANCE
                    .trim(1, player.getLocation().distance(closestPlayer.getLocation()));
                player.spigot().sendMessage(
                    Chat.INSTANCE.message("", "Located {player} {distance} blocks away",
                        "{player}", closestPlayer.getName(), "{distance}", distance));
                targets.put(player, closestPlayer);
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 0.5F);
                updateInventoryCompasses(player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null || event.getRecipe().getResult() == null
            || event.getInventory() == null) {
            return;
        }
        if (event.getRecipe().getResult().getType() != Material.COMPASS) {
            return;
        }
        event.getInventory().setResult(makeCompass(null));
    }

    @Override
    public void onLoad() {
        this.uhc.cooldownManager.displayCooldown(Material.COMPASS, this.cooldown);
        this.uhc.cooldownManager.registerNotifiable(this.cooldown);
        super.onLoad();
    }

    @Override
    public void onUnload() {
        this.uhc.cooldownManager.removeCooldown(Material.COMPASS);
        this.uhc.cooldownManager.unregisterNotifiable(this.cooldown);
        game.getTeams().values().forEach(t -> t.getPlayers().stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .forEach(this::clearCompassMetadata));
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.TWO_SECOND) {
            this.targets.forEach((tracker, tracked) -> {
                if (tracker.getLocation().getWorld().equals(tracked.getWorld())) {
                    tracker.setCompassTarget(tracked.getLocation());
                } else {
                    if (tracker.getBedSpawnLocation() != null) {
                        tracker.setCompassTarget(tracker.getBedSpawnLocation());
                    } else {
                        tracker
                            .setCompassTarget(tracker.getLocation().getWorld().getSpawnLocation());
                    }
                }
            });
        }
    }

    /**
     * Replace the compass(es) in the player's inventory with default compasses
     *
     * @param player The player
     */
    private void clearCompassMetadata(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null) {
                continue;
            }
            if (stack.getType() == Material.COMPASS) {
                ItemStack newCompass = new ItemStack(Material.COMPASS);
                inventory.setItem(i, newCompass);
            }
        }
        player.updateInventory();
    }

    /**
     * Finds the closest player to a location
     *
     * @param location The location
     * @param toIgnore A list of UUIDs to ignore
     *
     * @return The closest player, or null if none was found
     */
    private Player findClosestPlayer(Location location, Collection<UUID> toIgnore) {
        double closestDistance = Double.MAX_VALUE;
        Player foundPlayer = null;
        for (Player p : Bukkit.getOnlinePlayers().stream()
            .filter(p -> !toIgnore.contains(p.getUniqueId()))
            .filter(p -> p.getLocation().getWorld().equals(location.getWorld()))
            .collect(Collectors.toList())) {
            double distance = location.distanceSquared(p.getLocation());
            if (distance < closestDistance) {
                foundPlayer = p;
                closestDistance = distance;
            }
        }
        return foundPlayer;

    }

    /**
     * Makes a compass
     *
     * @param tracking The player to track
     *
     * @return The item
     */
    private ItemStack makeCompass(Player tracking) {
        String target = "Right Click";
        if (tracking != null) {
            target = "Tracking: " + tracking.getName();
        }
        return new ItemFactory(Material.COMPASS)
            .name(ChatColor.AQUA + "Player Tracker (" + ChatColor.GREEN + target + ChatColor.AQUA
                + ")")
            .lore("", ChatColor.GRAY + "Right click to set a target")
            .construct();
    }

    /**
     * Resets a player's compass to their bed spawn or world spawn
     *
     * @param p The player to reset
     */
    private void resetCompass(Player p) {
        if (p.getBedSpawnLocation() != null) {
            p.setCompassTarget(p.getBedSpawnLocation());
        } else {
            p.setCompassTarget(p.getWorld().getSpawnLocation());
        }
    }

    /**
     * Updates the player's compass(es) in their inventory
     *
     * @param player The player to update
     */
    private void updateInventoryCompasses(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null) {
                return;
            }
            if (stack.getType() == Material.COMPASS) {
                Player tracking = targets.get(player);
                inventory.setItem(i, makeCompass(tracking));
            }
        }
        player.updateInventory();
    }
}
