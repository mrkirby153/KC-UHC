package com.mrkirby153.kcuhc.game;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import java.util.Arrays;
import java.util.HashMap;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.flags.WorldFlags;
import me.mrkirby153.kcutils.protocollib.TitleTimings;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * The main game class
 */
public class UHCGame implements Listener {

    /**
     * The current state of the game
     */
    private GameState currentState = GameState.WAITING;

    /**
     * The main plugin instance
     */
    private UHC plugin;

    /**
     * A map of all the teams currently in the game
     */
    private HashMap<String, UHCTeam> teams = new HashMap<>();

    /**
     * The spectator team
     */
    private SpectatorTeam spectators;

    /**
     * The winning person/team to be displayed in the title
     */
    private String winner = "Nobody";

    /**
     * The amount of fireworks that were spawned
     */
    private int spawnedFireworks = 0;

    private Color fireworkColor = Color.WHITE;

    private long startTime = 0;

    private boolean generating = false;

    @Inject
    public UHCGame(UHC plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.spectators = new SpectatorTeam(plugin);
    }

    /**
     * Creates a team
     *
     * @param name  The name of the team
     * @param color The color of the team
     * @return The created team
     */
    public UHCTeam createTeam(String name, ChatColor color) {
        UHCTeam team = new UHCTeam(name, color);
        this.teams.put(name.toLowerCase(), team);
        return team;
    }

    public void deleteTeam(UHCTeam team) {
        this.teams.entrySet().removeIf(teamEntry -> teamEntry.getValue().equals(team));
    }

    public void generate() {
        if (generating)
            return; // Don't start another generation cycle
        ModuleRegistry.INSTANCE.getLoadedModule(WorldBorderModule.class).ifPresent(module -> {
            double wbSize = module.getStartSize() * (2D / 3);
            WorldBorder wb = UHC.getUHCWorld().getWorldBorder();
            int minX = (int) Math.ceil(wb.getCenter().getBlockX() - wbSize);
            int maxX = (int) Math.ceil(wb.getCenter().getBlockX() + wbSize);
            int minZ = (int) Math.ceil(wb.getCenter().getBlockZ() - wbSize);
            int maxZ = (int) Math.ceil(wb.getCenter().getBlockZ() + wbSize);

            new GenerationTask(plugin, UHC.getUHCWorld(), minX, maxX, minZ, maxZ, Void -> {
                this.generating = false;
                Bukkit.getServer().getOnlinePlayers().forEach(p -> {
                    p.sendMessage(Chat.INSTANCE.message("Pregeneration", "Pregeneration complete!").toLegacyText());
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F);
                });
            });
        });
    }

    /**
     * Gets the current state of the game
     *
     * @return The current state of the game
     */
    public GameState getCurrentState() {
        return currentState;
    }

    /**
     * Sets the current state of the game, firing {@link com.mrkirby153.kcuhc.game.event.GameStateChangeEvent}
     *
     * @param newState The new state of the game
     */
    public void setCurrentState(GameState newState) {
        plugin.getLogger().info(String.format("[GAME STATE] Changing from %s to %s", this.currentState, newState));
        this.currentState = newState;
        Bukkit.getServer().getPluginManager().callEvent(new GameStateChangeEvent(newState));
    }

    /**
     * Gets the spectator team
     *
     * @return The spectator team
     */
    public SpectatorTeam getSpectators() {
        return spectators;
    }

    /**
     * Gets the timestamp when the game started
     *
     * @return The game start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Gets the team the player is on
     *
     * @param player The Player to get the team for
     * @return The team, or null if the player isn't on a team
     */
    public ScoreboardTeam getTeam(Player player) {
        for (UHCTeam team : this.teams.values()) {
            if (team.getPlayers().contains(player.getUniqueId()))
                return team;
        }
        if (this.spectators.getPlayers().contains(player.getUniqueId()))
            return this.spectators;
        return null;
    }

    /**
     * Gets a team by its name
     *
     * @param name The name of the team
     * @return The team, or null if it doesn't exist
     */
    public UHCTeam getTeam(String name) {
        return teams.get(name.toLowerCase());
    }

    /**
     * Gets all the teams currently registered
     *
     * @return The team
     */
    public HashMap<String, UHCTeam> getTeams() {
        return teams;
    }

    /**
     * Checks if the player is a spectator
     *
     * @param player The player to check
     * @return True if the player is a spectator, false if otherwise
     */
    public boolean isSpectator(Player player) {
        ScoreboardTeam team = getTeam(player);
        return team != null && team instanceof SpectatorTeam;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.COUNTDOWN) {
            new CountdownTimer(plugin, 10, 20, time -> {
                if (time == 0) {
                    setCurrentState(GameState.ALIVE);
                    return;
                }
                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.spigot().sendMessage(Chat.INSTANCE.message("Game", "Starting in {time} seconds", "{time}", time));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F);
                });
            });
        }
        if (event.getTo() == GameState.ALIVE) {
            Bukkit.getOnlinePlayers().stream().filter(p -> !this.spectators.getPlayers().contains(p.getUniqueId())).filter(Player::isValid).forEach(p -> {
                p.setAllowFlight(false);
                p.setFlying(false);
                p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                p.setFoodLevel(20);
                p.setExhaustion(0);

                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30 * 20, 5, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 5, true));
            });
            UHC.getUHCWorld().setGameRuleValue("doDaylightCycle", "true");
            UHC.getUHCWorld().setTime(0);
        }
        if (event.getTo() == GameState.ENDING || event.getTo() == GameState.WAITING) {
            Arrays.stream(WorldFlags.values()).forEach(f -> plugin.flagModule.set(UHC.getUHCWorld(), f, false, false));
            UHC.getUHCWorld().setGameRuleValue("doDaylightCycle", "false");
            UHC.getUHCWorld().setTime(1200);
        }
        if (event.getTo() == GameState.ALIVE) {
            Arrays.stream(WorldFlags.values()).forEach(f -> plugin.flagModule.set(UHC.getUHCWorld(), f, true, false));
            this.startTime = System.currentTimeMillis();
        }
        if (event.getTo() == GameState.ENDING) {
            // Teleport everyone to the center
            Location toTeleport = UHC.getUHCWorld().getWorldBorder().getCenter();
            toTeleport = toTeleport.getWorld().getHighestBlockAt(toTeleport).getLocation().add(0, 0.5, 0);
            Location finalToTeleport = toTeleport;
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!player.isValid())
                    return;
                player.teleport(finalToTeleport);
                ScoreboardTeam team = getTeam(player);
                if (team != null)
                    team.removePlayer(player);
                player.getInventory().clear();
                player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
                plugin.protocolLibManager.title(player, ChatColor.GOLD + winner, "won the game", new TitleTimings(20, 60, 20));
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (getTeam(event.getEntity()) != null)
            getTeam(event.getEntity()).removePlayer(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Join the spectator team
        Bukkit.getServer().getScheduler().runTask(plugin, () -> {
            if (getTeam(event.getPlayer()) != null)
                getTeam(event.getPlayer()).removePlayer(event.getPlayer());
            this.spectators.addPlayer(event.getPlayer());
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.SECOND) {
            if (getCurrentState() == GameState.WAITING || getCurrentState() == GameState.ENDING || getCurrentState() == GameState.ENDED) {
                Bukkit.getOnlinePlayers().stream().filter(Player::isValid).forEach(p -> {
                    if (!p.getAllowFlight())
                        p.setAllowFlight(true);
                    p.setFoodLevel(20);
                    p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                });
            }
        }
        if (event.getType() == UpdateType.SLOW) {
            if (getCurrentState() == GameState.ENDING) {
                // Spawn the fireworks
                if (this.spawnedFireworks++ < 8) {
                    spawnFireworks(UHC.getUHCWorld());
                } else {
                    setCurrentState(GameState.ENDED);
                }
            }
        }
    }

    /**
     * Stops the game with the specified winner
     *
     * @param winner    The winner
     * @param teamColor The color of the fireworks
     */
    public void stop(String winner, Color teamColor) {
        this.winner = winner;
        this.spawnedFireworks = 0;
        this.fireworkColor = teamColor;
        setCurrentState(GameState.ENDING);
    }

    /**
     * Spawns the fireworks for the endgame
     *
     * @param world The world to spawn in
     */
    private void spawnFireworks(World world) {
        Location center = world.getWorldBorder().getCenter();
        center = world.getHighestBlockAt(center).getLocation();
        int distFromWB = 16;
        double worldborderRadius = world.getWorldBorder().getSize() / 2d;
        Location pZX = center.clone().add(worldborderRadius - distFromWB + (5 * Math.random()), 20, worldborderRadius - distFromWB + (5 * Math.random()));
        Location pXnZ = center.clone().add(worldborderRadius - distFromWB + (5 * Math.random()), 20, -(worldborderRadius - distFromWB + (5 * Math.random())));
        Location pZnX = center.clone().add(-(worldborderRadius - distFromWB) + (5 * Math.random()), 20, worldborderRadius - distFromWB + (5 * Math.random()));
        Location nXZ = center.clone().add(-(worldborderRadius - distFromWB) + (5 * Math.random()), 20, -(worldborderRadius - distFromWB + (5 * Math.random())));

        Firework fw_pZX = (Firework) world.spawnEntity(pZX, EntityType.FIREWORK);
        Firework fw_pXnZ = (Firework) world.spawnEntity(pXnZ, EntityType.FIREWORK);
        Firework fw_pZnX = (Firework) world.spawnEntity(pZnX, EntityType.FIREWORK);
        Firework fw_nXZ = (Firework) world.spawnEntity(nXZ, EntityType.FIREWORK);

        FireworkMeta meta = fw_pZX.getFireworkMeta();
        FireworkEffect.Type type = FireworkEffect.Type.BALL_LARGE;
        FireworkEffect eff = FireworkEffect.builder().flicker(true).withColor(this.fireworkColor).with(type).trail(true).build();
        meta.addEffect(eff);
        meta.setPower(1);
        fw_pZX.setFireworkMeta(meta);
        fw_pXnZ.setFireworkMeta(meta);
        fw_pZnX.setFireworkMeta(meta);
        fw_nXZ.setFireworkMeta(meta);
    }
}
