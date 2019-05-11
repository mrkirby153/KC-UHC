package com.mrkirby153.kcuhc.game;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.event.GameStartingEvent;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.event.GameStoppingEvent;
import com.mrkirby153.kcuhc.game.event.GameStoppingEvent.Reason;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.flags.WorldFlags;
import me.mrkirby153.kcutils.protocollib.TitleTimings;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    /**
     * The name of the world that the UHC game will take place in
     */
    private String uhcWorld = "world";

    private long initialPlayers = 0;

    private Map<UUID, Long> killCount = new HashMap<>();

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
     *
     * @return The created team
     */
    public UHCTeam createTeam(String name, ChatColor color) {
        UHCTeam team = new UHCTeam(name, color, this);
        this.teams.put(name.toLowerCase(), team);
        return team;
    }

    public void deleteTeam(UHCTeam team) {
        this.teams.entrySet().removeIf(teamEntry -> teamEntry.getValue().equals(team));
    }

    public void generate() {
        if (generating) {
            return; // Don't start another generation cycle
        }
        ModuleRegistry.INSTANCE.getLoadedModule(WorldBorderModule.class).ifPresent(module -> {
            double wbSize = module.getStartSize() * (2D / 3);
            WorldBorder wb = this.getWorldBorder();
            int minX = (int) Math.ceil(wb.getCenter().getBlockX() - wbSize);
            int maxX = (int) Math.ceil(wb.getCenter().getBlockX() + wbSize);
            int minZ = (int) Math.ceil(wb.getCenter().getBlockZ() - wbSize);
            int maxZ = (int) Math.ceil(wb.getCenter().getBlockZ() + wbSize);

            new GenerationTask(plugin, this.getUHCWorld(), minX, maxX, minZ, maxZ, Void -> {
                this.generating = false;
                Bukkit.getServer().getOnlinePlayers().forEach(p -> {
                    p.sendMessage(Chat.message("Pregeneration", "Pregeneration complete!")
                        .toLegacyText());
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1F, 1F);
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
        plugin.getLogger().info(
            String.format("[GAME STATE] Changing from %s to %s", this.currentState, newState));
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
     *
     * @return The team, or null if the player isn't on a team
     */
    public ScoreboardTeam getTeam(Player player) {
        for (UHCTeam team : this.teams.values()) {
            if (team.getPlayers().contains(player.getUniqueId())) {
                return team;
            }
        }
        if (this.spectators.getPlayers().contains(player.getUniqueId())) {
            return this.spectators;
        }
        return null;
    }

    /**
     * Gets a team by its name
     *
     * @param name The name of the team
     *
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
     * Starts the game
     *
     * @return True if the game was started successfully
     */
    public boolean start() {
        this.plugin.getLogger().info("Starting game");
        GameStartingEvent event = new GameStartingEvent();
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.plugin.getLogger().info("Game start canceled");
            return false;
        }
        this.setCurrentState(GameState.COUNTDOWN);
        return true;
    }

    /**
     * Abort the currently running game
     */
    public boolean abort() {
        this.plugin.getLogger().info("!!! ABORTING GAME !!!");
        if (currentState != GameState.ALIVE) {
            return false; // Can't abort a game if it's not running :meowthinksmart:
        }
        GameStoppingEvent event = new GameStoppingEvent(Reason.ABORTED);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.plugin.getLogger().info("Game abort canceled");
            return false;
        }
        this.stop("Nobody", Color.WHITE);
        return true;
    }

    /**
     * Checks if the player is a spectator
     *
     * @param player The player to check
     *
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
                    if (time <= 5) {
                        ChatColor color = time <= 3 ? ChatColor.RED : ChatColor.YELLOW;
                        p.sendTitle(color + "" + time, "", 5, 21, 5);
                    }
                    p.spigot().sendMessage(Chat
                        .message("Game", "Starting in {time} seconds", "{time}", time));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1F, 1F);
                });
            });
        }
        if (event.getTo() == GameState.ALIVE) {
            // Butcher all the mobs
            getUHCWorld().getEntities().forEach(m -> {
                if (m instanceof Monster) {
                    plugin.getLogger().info("butchered " + m.getType());
                    m.remove();
                }
            });
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> !this.spectators.getPlayers().contains(p.getUniqueId()))
                .filter(Player::isValid).forEach(p -> {
                p.setAllowFlight(false);
                p.setFlying(false);
                p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                p.setFoodLevel(20);
                p.setExhaustion(0);

                p.addPotionEffect(
                    new PotionEffect(PotionEffectType.REGENERATION, 30 * 20, 5, true));
                p.addPotionEffect(
                    new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 5, true));
            });
            this.getUHCWorld().setGameRuleValue("doDaylightCycle", "true");
            this.getUHCWorld().setTime(0);
        }
        if (event.getTo() == GameState.ENDING || event.getTo() == GameState.WAITING) {
            Arrays.stream(WorldFlags.values())
                .forEach(f -> plugin.flagModule.set(this.getUHCWorld(), f, false, false));
            this.getUHCWorld().setGameRuleValue("doDaylightCycle", "false");
            this.getUHCWorld().setTime(1200);
        }
        if (event.getTo() == GameState.ALIVE) {
            Arrays.stream(WorldFlags.values())
                .forEach(f -> plugin.flagModule.set(this.getUHCWorld(), f, true, false));
            this.startTime = System.currentTimeMillis();
            this.initialPlayers = this.teams.values().stream().mapToLong(t -> t.getPlayers().size())
                .sum();
        }
        if (event.getTo() == GameState.ENDING) {
            // Teleport everyone to the center
            Location toTeleport = this.getUHCWorld().getWorldBorder().getCenter();
            toTeleport = toTeleport.getWorld().getHighestBlockAt(toTeleport).getLocation()
                .add(0, 0.5, 0);
            Location finalToTeleport = toTeleport;
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!player.isValid()) {
                    return;
                }
                player.teleport(finalToTeleport);
                ScoreboardTeam team = getTeam(player);
                if (team != null) {
                    team.removePlayer(player);
                }
                player.getInventory().clear();
                player.getActivePotionEffects()
                    .forEach(e -> player.removePotionEffect(e.getType()));
                plugin.protocolLibManager.title(player, ChatColor.GOLD + winner, "won the game",
                    new TitleTimings(20, 60, 20));
            });
        }
    }

    /**
     * Gets the world that the UHC game will take place in
     *
     * @return The world that the UHC game is taking place in
     */
    public World getUHCWorld() {
        return Bukkit.getWorld(this.uhcWorld);
    }

    /**
     * Gets the nether world corresponding to the UHC world
     *
     * @return The nether world
     */
    public World getUHCNether() {
        return Bukkit.getWorld(this.uhcWorld + "_the_nether");
    }

    /**
     * Gets the initial players in the game
     *
     * @return The initial amount of players
     */
    public long getInitialPlayers() {
        return this.initialPlayers;
    }

    /**
     * Gets a player's kills
     *
     * @param player The player
     *
     * @return The amount of kills
     */
    public long getKills(Player player) {
        if (this.killCount == null) {
            this.killCount = new HashMap<>();
        }
        return this.killCount.getOrDefault(player.getUniqueId(), 0L);
    }

    /**
     * Gets the world border for the UHC world
     *
     * @return The world border
     */
    public WorldBorder getWorldBorder() {
        return this.getUHCWorld().getWorldBorder();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (getTeam(event.getEntity()) != null) {
            getTeam(event.getEntity()).removePlayer(event.getEntity());
        }
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            if (this.killCount == null) {
                this.killCount = new HashMap<>();
            }
            long count = this.killCount
                .computeIfAbsent(killer.getUniqueId(), uuid -> 0L);
            this.killCount.put(killer.getUniqueId(), ++count);
        }
        String deathMessage = event.getDeathMessage();
        if (deathMessage != null) {
            deathMessage = deathMessage.replace(event.getEntity().getName(), "{entity}");
            String k = "";
            if (killer != null) {
                k = killer.getName();
                deathMessage = deathMessage.replace(killer.getName(), "{killer}");
            }
            event.setDeathMessage(
                Chat.message("Death", deathMessage, "{entity}", event.getEntity().getName(),
                    "{killer}", k).toLegacyText());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Join the spectator team
        Bukkit.getServer().getScheduler().runTask(plugin, () -> {
            if (getTeam(event.getPlayer()) != null) {
                getTeam(event.getPlayer()).removePlayer(event.getPlayer());
            }
            this.spectators.addPlayer(event.getPlayer());
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.SECOND) {
            if (getCurrentState() == GameState.WAITING || getCurrentState() == GameState.ENDING
                || getCurrentState() == GameState.ENDED) {
                Bukkit.getOnlinePlayers().stream().filter(Player::isValid).forEach(p -> {
                    if (!p.getAllowFlight()) {
                        p.setAllowFlight(true);
                    }
                    p.setFoodLevel(20);
                    p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                });
            }
        }
        if (event.getType() == UpdateType.SLOW) {
            if (getCurrentState() == GameState.ENDING) {
                // Spawn the fireworks
                if (this.spawnedFireworks++ < 8) {
                    spawnFireworks(this.getUHCWorld());
                } else {
                    setCurrentState(GameState.ENDED);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(Chat
            .message("Join", "{player} joined!", "{player}", event.getPlayer().getName())
            .toLegacyText());
        if (getCurrentState() == GameState.WAITING) {
            // Teleport the user to spawn
            event.getPlayer().teleport(getUHCWorld().getSpawnLocation());
        }
        event.getPlayer().updateCommands(); // Ensure the command list is up-to-date
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(Chat
            .message("Leave", "{player} left!", "{player}", event.getPlayer().getName())
            .toLegacyText());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getMessage().startsWith("$$") && event.getPlayer().isOp()) {
            event.setMessage(event.getMessage().substring(2));
            event.setCancelled(true);
            Bukkit.getOnlinePlayers().forEach(p -> {
                String[] parts = event.getMessage().split("\\|");
                if (parts.length < 2) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 1F);
                } else {
                    p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1F, 1F);
                }

                if (!plugin.protocolLibManager.isErrored() && parts.length == 2) {
                    plugin.protocolLibManager
                        .title(p, ChatColor.RED + parts[0].trim(), ChatColor.GOLD + parts[1].trim(),
                            new TitleTimings(20, 120, 10));
                    p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + parts[0].trim() + ": "
                        + ChatColor.GOLD + parts[1].trim());
                } else {
                    p.sendMessage(
                        ChatColor.GREEN + "" + ChatColor.BOLD + "ANNOUNCEMENT> " + ChatColor.YELLOW
                            + event.getPlayer().getName() + " " + ChatColor.LIGHT_PURPLE + event
                            .getMessage());
                }
            });
        }
        ScoreboardTeam team = this.getTeam(event.getPlayer());
        ChatColor color = ChatColor.GRAY;
        if (team != null) {
            color = team.getColor();
        }
        event.setFormat("<" + color + "%s" + ChatColor.RESET + "> %s");
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
        Location pZX = center.clone().add(worldborderRadius - distFromWB + (5 * Math.random()), 20,
            worldborderRadius - distFromWB + (5 * Math.random()));
        Location pXnZ = center.clone().add(worldborderRadius - distFromWB + (5 * Math.random()), 20,
            -(worldborderRadius - distFromWB + (5 * Math.random())));
        Location pZnX = center.clone()
            .add(-(worldborderRadius - distFromWB) + (5 * Math.random()), 20,
                worldborderRadius - distFromWB + (5 * Math.random()));
        Location nXZ = center.clone()
            .add(-(worldborderRadius - distFromWB) + (5 * Math.random()), 20,
                -(worldborderRadius - distFromWB + (5 * Math.random())));

        Firework fw_pZX = (Firework) world.spawnEntity(pZX, EntityType.FIREWORK);
        Firework fw_pXnZ = (Firework) world.spawnEntity(pXnZ, EntityType.FIREWORK);
        Firework fw_pZnX = (Firework) world.spawnEntity(pZnX, EntityType.FIREWORK);
        Firework fw_nXZ = (Firework) world.spawnEntity(nXZ, EntityType.FIREWORK);

        FireworkMeta meta = fw_pZX.getFireworkMeta();
        FireworkEffect.Type type = FireworkEffect.Type.BALL_LARGE;
        FireworkEffect eff = FireworkEffect.builder().flicker(true).withColor(this.fireworkColor)
            .with(type).trail(true).build();
        meta.addEffect(eff);
        meta.setPower(1);
        fw_pZX.setFireworkMeta(meta);
        fw_pXnZ.setFireworkMeta(meta);
        fw_pZnX.setFireworkMeta(meta);
        fw_nXZ.setFireworkMeta(meta);
    }
}
