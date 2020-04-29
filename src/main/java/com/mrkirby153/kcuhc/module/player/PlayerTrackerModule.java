package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.settings.IntegerSetting;
import com.mrkirby153.kcuhc.player.ActionBar;
import com.mrkirby153.kcuhc.player.ActionBarManager;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.Time.TimeUnit;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import net.md_5.bungee.api.chat.TextComponent;
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

    private final ActionBar cooldownActionBar = new ActionBar("cooldown", 2);
    private Map<Player, Player> targets = new HashMap<>();
    private UHC uhc;
    private UHCGame game;
    private IntegerSetting cooldown = new IntegerSetting(30);
    private Map<UUID, Long> compassCooldowns = new HashMap<>();


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
            if (!checkCooldown(player)) {
                player.spigot().sendMessage(Chat
                    .message("Tracker", "You can use this again in {time}", "{time}", Time.INSTANCE
                        .format(1, getTimeLeft(player), TimeUnit.FIT)));
                return;
            } else {
                resetCooldown(player);
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
                player.spigot().sendMessage(Chat.error("Could not find a player"));
                this.targets.remove(player);
                this.resetCompass(player);
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 2F);
            } else {
                double distance = Time.INSTANCE
                    .trim(1, player.getLocation().distance(closestPlayer.getLocation()));
                player.spigot().sendMessage(
                    Chat.message("", "Located {player} {distance} blocks away",
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
        ActionBarManager.getInstance().registerActionBar(cooldownActionBar);
    }

    @Override
    public void onUnload() {
        game.getTeams().values().forEach(t -> t.getPlayers().stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .forEach(this::clearCompassMetadata));
        ActionBarManager.getInstance().unregisterActionBar(cooldownActionBar);
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
        if (event.getType() == UpdateType.TICK) {
            Bukkit.getOnlinePlayers().forEach(this::updateCooldownDisplay);
            Bukkit.getOnlinePlayers().forEach(player ->{
                if(compassCooldowns.containsKey(player.getUniqueId())) {
                    if(getTimeLeft(player) <= 0) {
                        player.sendMessage(Chat.message("Player tracker recharged").toLegacyText());
                        compassCooldowns.remove(player.getUniqueId());
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

    private boolean checkCooldown(Player player) {
        if(!compassCooldowns.containsKey(player.getUniqueId()))
            return true;
        return System.currentTimeMillis() > compassCooldowns.get(player.getUniqueId());
    }

    private void resetCooldown(Player player) {
        compassCooldowns
            .put(player.getUniqueId(), System.currentTimeMillis() + (cooldown.getValue() * 1000));
    }

    private long getTimeLeft(Player player) {
        if (!compassCooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }
        long coolsOffAt = compassCooldowns.get(player.getUniqueId());
        return coolsOffAt - System.currentTimeMillis();
    }

    private void updateCooldownDisplay(Player player) {
        if (getTimeLeft(player) <= 0) {
            if (cooldownActionBar.get(player) != null) {
                cooldownActionBar.clear(player);
            }
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS
            && player.getInventory().getItemInOffHand().getType() != Material.COMPASS) {
            cooldownActionBar.clear(player);
            return;
        }
        long timeLeft = getTimeLeft(player);
        double cooldownTimeMs = cooldown.getValue() * 1000D;
        double percentLeft = (cooldownTimeMs - timeLeft) / cooldownTimeMs;

        double filledBars = Math.floor(percentLeft * 10);

        TextComponent c = Chat
            .formattedChat(Time.INSTANCE.format(1, timeLeft, TimeUnit.FIT, TimeUnit.SECONDS),
                net.md_5.bungee.api.ChatColor.GREEN);
        c.addExtra(Chat.formattedChat(" [", net.md_5.bungee.api.ChatColor.WHITE));
        for (int i = 1; i <= 10; i++) {
            TextComponent c1 = new TextComponent("█");
            if (i <= filledBars) {
                c1.setColor(net.md_5.bungee.api.ChatColor.GREEN);
            } else {
                c1.setColor(net.md_5.bungee.api.ChatColor.RED);
            }
            c.addExtra(c1);
        }
        c.addExtra(Chat.formattedChat("]", net.md_5.bungee.api.ChatColor.WHITE));
        cooldownActionBar.set(player, c);
    }
}
