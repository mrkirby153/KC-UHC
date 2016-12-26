package me.mrkirby153.kcuhc.arena;


import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.gui.SpecInventory;
import me.mrkirby153.kcuhc.handler.FreezeHandler;
import me.mrkirby153.kcuhc.handler.MOTDHandler;
import me.mrkirby153.kcuhc.handler.listener.GameListener;
import me.mrkirby153.kcuhc.handler.listener.PregameListener;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.player.LoneWolfModule;
import me.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcuhc.team.LoneWolfTeam;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.TeamSpectator;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTime;
import me.mrkirby153.kcuhc.utils.UtilTitle;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandAssignSpectator;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandLoneWolf;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandToLobby;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandAssignTeams;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandNewTeam;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandRemoveTeam;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftEntity;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static me.mrkirby153.kcuhc.UHC.uhcNetwork;
import static me.mrkirby153.kcuhc.arena.UHCArena.State.*;
import static me.mrkirby153.kcuhc.utils.UtilChat.generateBoldChat;

public class UHCArena implements Runnable, Listener {


    private static final int WORLDBORDER_WARN_DIST = 50;
    private static final int TIP_TIME = 30000;
    // Worlds

    public ScoreboardUpdater scoreboardUpdater;
    protected String winner;
    protected long startTime = 0;
    CountdownBarTask countdownTask;
    private ArenaProperties properties;
    private boolean firstAnnounce = true;
    private boolean shouldAnnounce = false;
    private String lastAnnounced = "-1";

    private ArrayList<Player> players = new ArrayList<>();

    private State state = State.INITIALIZED;

    private int generationTaskId;
    private int countdown;


    private int launchedFw = 0;
    private Color winningTeamColor = Color.WHITE;

    private List<Player> previouslyOpped = new ArrayList<>();

    private HashMap<UUID, String> uuidToStringMap = new HashMap<>();
    private HashMap<UUID, Long> logoutTimes = new HashMap<>();
    private ArrayList<UUID> queuedTeamRemovals = new ArrayList<>();

    private int secondsRemaining;
    private double overworldWorldborderSize;
    private double netherWorldborderSize;
    private long freezeStartTime;

    private boolean notifiedDisabledSpawn = false;

    private String[] tips = new String[]{
            ChatColor.GOLD + "Craft a Head Apple by surrounding a player head in gold!",
            ChatColor.YELLOW + "Use a player tracker (Compass) to find enemies!",
            ChatColor.GOLD + "5 Minutes after the worldborder stops, everyone gets Hunger III",
            ChatColor.YELLOW + "Watch out for the worldborder! It can sneak up on you quickly!",
            ChatColor.GOLD + "%NAME% is going to win!",
            ChatColor.YELLOW + "This tip message format was NOT stolen from Mineplex",
            ChatColor.GOLD + "Message'); DROP TABLE `tips`; --",
            ChatColor.GOLD + "It may or may not be a good idea to not die",
            ChatColor.YELLOW + "This is not the tip you are looking for",
            ChatColor.BLUE + "Hey! Isn't this tip supposed to be yellow or gold?",
            ChatColor.GOLD + "Was I supposed to write legitimate tips? Oops.",
            ChatColor.YELLOW + "You mean whatever I write here will show up as a tip? HI MOM",
            ChatColor.GOLD + "In the event of an emergency, do not panic! Use /a <message> to contact an admin!",
            ChatColor.YELLOW + "Access your team-specific inventory with " + ChatColor.GREEN + "/teaminv",
            ChatColor.GOLD + "Talk privately with your team by prefixing your message with an " + ChatColor.GREEN + "@!"

    };

    private long nextTipIn = -1;
    private int runCount = 0;

    private long graceUntil = -1;


    private TeamHandler teamHandler;

    private UHC plugin;

    public UHCArena(UHC plugin, TeamHandler teamHandler, String presetFile) {
        if (presetFile == null)
            presetFile = "default";
        this.teamHandler = teamHandler;
        this.properties = ArenaProperties.loadProperties(presetFile);
        this.plugin = plugin;

        this.scoreboardUpdater = new ScoreboardUpdater(this, teamHandler, new UHCScoreboard(plugin));

        plugin.getServer().getPluginManager().registerEvents(new PregameListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GameListener(teamHandler, plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 20);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, scoreboardUpdater::refresh, 0, 1L);

    }

    public UHCArena(UHC plugin, TeamHandler teamHandler) {
        this(plugin, teamHandler, "default");
    }

    public void addPlayer(Player player) {
        // Remove old player object
        players.removeIf(player1 -> player1.getUniqueId().equals(player.getUniqueId()));
        this.players.add(player);
    }

    public void bringEveryoneToLobby() {
        if (UHC.uhcNetwork != null) {
            new BotCommandToLobby(plugin.serverId()).publish();
            teamHandler.teams().forEach(t -> new BotCommandRemoveTeam(plugin.serverId(), t.getTeamName()).publish());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void chatEvent(AsyncPlayerChatEvent event) {
        UHCTeam team = teamHandler.getTeamForPlayer(event.getPlayer());
        if (team != null) {
            if (team instanceof TeamSpectator) {
                event.setFormat(ChatColor.GRAY + "Spectator %s " + ChatColor.WHITE + "%s");
            } else {
                event.setFormat(team.getColor() + "%s " + ChatColor.WHITE + "%s");
            }
        } else {
            event.setFormat(ChatColor.GRAY + "%s " + ChatColor.WHITE + "%s");
        }
    }



    public State currentState() {
        return this.state;
    }

    public void freeze() {
        int secondsPassed = Math.toIntExact((System.currentTimeMillis() - startTime) / 1000);

        System.out.println("Duration: " + properties.WORLDBORDER_TRAVEL_TIME.get() * 60);
        System.out.println("Seconds passed: " + secondsPassed);
        this.secondsRemaining = properties.WORLDBORDER_TRAVEL_TIME.get() * 60 - secondsPassed;

        System.out.println("Seconds remaining: " + secondsRemaining);
        overworldWorldborderSize = getWorld().getWorldBorder().getSize();
        getWorld().getWorldBorder().setSize(overworldWorldborderSize);
        System.out.println("Worldborder size: " + overworldWorldborderSize);

        freezeStartTime = System.currentTimeMillis();

        if (getNether() != null) {
            netherWorldborderSize = getNether().getWorldBorder().getSize();
            getNether().getWorldBorder().setSize(netherWorldborderSize);
        }
        this.state = FROZEN;
        Bukkit.getOnlinePlayers().stream().filter(p -> !teamHandler.isSpectator(p)).forEach(FreezeHandler::freezePlayer);
        getWorld().getEntities().stream().filter(e -> e.getType() != EntityType.PLAYER).forEach(e -> FreezeHandler.frozenEntities.put(e, e.getLocation()));

        getWorld().setGameRuleValue("doDaylightCycle", "false");
        FreezeHandler.pvpEnabled = false;

        Bukkit.broadcastMessage(UtilChat.message("The game is now frozen"));
    }

    public void generate() {
        MOTDHandler.setMotd(ChatColor.DARK_RED + "Pregenerating world, come back soon!");
        setState(State.GENERATING_WORLD);

        Integer worldborderSize = properties.WORLDBORDER_START_SIZE.get();
        int minX = getCenter().getBlockX() - worldborderSize;
        int maxX = getCenter().getBlockX() + worldborderSize;
        int minZ = getCenter().getBlockZ() - worldborderSize;
        int maxZ = getCenter().getBlockZ() + worldborderSize;

        generationTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin,
                new GenerationTask(plugin, this, getWorld(), minX / 512, minZ / 512, maxX / 512, maxZ / 512), 1L, 1L);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer(net.md_5.bungee.api.ChatColor.RED + "We are pregenerating the world, come back later");
        }
    }

    public void generationComplete() {
        setState(State.WAITING);
        MOTDHandler.setMotd(ChatColor.GRAY + "Pending initialization");
        Bukkit.broadcastMessage(UtilChat.message("World generation complete"));
        plugin.getServer().getScheduler().cancelTask(generationTaskId);
        initialize();
    }

    public Location getCenter() {
        return getWorld().getHighestBlockAt((int) properties.WORLDBORDER_CENTER.get().getX(), (int) properties.WORLDBORDER_CENTER.get().getZ()).getLocation();
    }

    public World getNether() {
        return Bukkit.getWorld(getWorld().getName() + "_nether");
    }

    public ArenaProperties getProperties() {
        return properties;
    }

    public void setProperties(ArenaProperties properties) {
        this.properties = properties;
    }

    public World getWorld() {
        return Bukkit.getWorld(properties.WORLD.get());
    }

    public void initialize() {
        getWorld().setGameRuleValue("doDaylightCycle", "false");
        getWorld().setGameRuleValue("doMobSpawning", "false");
        getWorld().setGameRuleValue("doMobLoot", "false");
        getWorld().setTime(1200);
        Bukkit.broadcastMessage(UtilChat.message("Initialized"));
        MOTDHandler.setMotd(ChatColor.GREEN + "Ready");
        players = new ArrayList<>();
        launchedFw = 0;
        winningTeamColor = Color.WHITE;
        setState(INITIALIZED);
        queuedTeamRemovals.clear();
        uuidToStringMap.clear();
        logoutTimes.clear();
        players.clear();
        previouslyOpped.clear();
    }
    public void playerDisconnect(Player player) {
        UHCTeam teamForPlayer = teamHandler.getTeamForPlayer(player);
        if (teamForPlayer == null || teamForPlayer instanceof TeamSpectator) {
            players.remove(player);
            return;
        }
        uuidToStringMap.put(player.getUniqueId(), player.getName());
        int reconnectMinutes = properties.REJOIN_MINUTES.get();
        int reconnectMs = 1000 * 60 * reconnectMinutes;
        logoutTimes.put(player.getUniqueId(), System.currentTimeMillis() + reconnectMs);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + player.getName() + " has disconnected! " + UtilTime.format(1, reconnectMs, UtilTime.TimeUnit.FIT) + " to rejoin!");
        }
    }

    public void playerJoin(Player player) {
        if (logoutTimes.containsKey(player.getUniqueId())) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + player.getName() + " has reconnected!");
            }
            this.logoutTimes.remove(player.getUniqueId());
        }
        if (queuedTeamRemovals.contains(player.getUniqueId())) {
            queuedTeamRemovals.remove(player.getUniqueId());
            teamHandler.leaveTeam(player);
            players.remove(player);
            spectate(player);
            player.sendMessage(UtilChat.message("You have disconnected more than five minutes ago and have been removed from the game"));
        }
    }

    public Player[] players() {
        return players.toArray(new Player[players.size()]);
    }

    public void removePlayer(Player player) {
        Iterator<Player> players = this.players.iterator();
        while (players.hasNext()) {
            if (players.next().getUniqueId().equals(player.getUniqueId()))
                players.remove();
        }
    }

    @Override
    public void run() {
        switch (state) {
            case WAITING:
            case INITIALIZED:
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setAllowFlight(true);
                }
                if (System.currentTimeMillis() > nextTipIn) {
                    displayTip();
                }
                break;
            case COUNTDOWN:
                if (countdownTask == null) {
                    countdownTask = new CountdownBarTask(plugin, System.currentTimeMillis() + 10000, 10000);
                    countdownTask.setTaskId(plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, countdownTask, 0L, 1L));
                }
                if (countdown > 0) {
                    for (Player p : players) {

                        if (countdown <= 3) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1, 0.5F);
                            UtilTitle.title(p, ChatColor.RED + String.valueOf(countdown), "", 0, 25, 0);
                        } else {
                            UtilTitle.title(p, ChatColor.GOLD + "Starting in", ChatColor.RED + "" + countdown, 0, 25, 0);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1, 1);
                        }
                        p.sendMessage(UtilChat.message("Starting in " + ChatColor.GOLD + countdown));
                    }
                    MOTDHandler.setMotd(ChatColor.YELLOW + "Starting in " + countdown);
                    countdown--;
                } else {
                    start();
                }
                break;
            case RUNNING:
                MOTDHandler.setMotd(ChatColor.RED + "Game in progress. " + ChatColor.AQUA + "" + (players.size() - getSpectatorCount()) + ChatColor.RED + " alive");
                for (Player p : players) {
                    p.setGlowing(false);
                }
                if (teamCountLeft() <= 1 && properties.CHECK_ENDING.get()) {
                    if (teamsLeft().size() > 0) {
                        UHCTeam team = teamsLeft().get(0);
                        if (team instanceof LoneWolfTeam) {
                            if (team.getPlayers().size() <= 1) {
                                this.winningTeamColor = team.toColor();
                                stop(team.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList()).get(0).getDisplayName());
                            }
                        } else {
                            this.winningTeamColor = team.toColor();
                            stop(team.getFriendlyName());
                        }
                    }
                }
                if (ModuleRegistry.isLoaded(WorldBorderModule.class)) {
                    Optional<WorldBorderModule> optWorldborderMod = ModuleRegistry.getLoadedModule(WorldBorderModule.class);
                    if(optWorldborderMod.isPresent()){
                        WorldBorderModule worldBorderModule = optWorldborderMod.get();
                        worldBorderModule.setWarningDistance(0);
                        if(!notifiedDisabledSpawn && worldBorderModule.travelComplete()){
                            for(Player p : Bukkit.getOnlinePlayers()){
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F);
                                p.sendMessage(UtilChat.message("Natural mob spawning has been cut by 75%"));
                            }
                            notifiedDisabledSpawn = true;
                        }
                    }
                }
                break;
            case ENDGAME:
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setAllowFlight(true);
                    p.getInventory().clear();
                    teamHandler.leaveTeam(p);
                }
                if (this.launchedFw++ < 8) {
                    int distFromWB = 16;
                    double worldborderRadius = getWorld().getWorldBorder().getSize() / 2d;
                    Location pZX = getCenter().clone().add(worldborderRadius - distFromWB + (5 * Math.random()), 20, worldborderRadius - distFromWB + (5 * Math.random()));
                    Location pXnZ = getCenter().clone().add(worldborderRadius - distFromWB + (5 * Math.random()), 20, -(worldborderRadius - distFromWB + (5 * Math.random())));
                    Location pZnX = getCenter().clone().add(-(worldborderRadius - distFromWB) + (5 * Math.random()), 20, worldborderRadius - distFromWB + (5 * Math.random()));
                    Location nXZ = getCenter().clone().add(-(worldborderRadius - distFromWB) + (5 * Math.random()), 20, -(worldborderRadius - distFromWB + (5 * Math.random())));

                    Firework fw_pZX = (Firework) getWorld().spawnEntity(pZX, EntityType.FIREWORK);
                    Firework fw_pXnZ = (Firework) getWorld().spawnEntity(pXnZ, EntityType.FIREWORK);
                    Firework fw_pZnX = (Firework) getWorld().spawnEntity(pZnX, EntityType.FIREWORK);
                    Firework fw_nXZ = (Firework) getWorld().spawnEntity(nXZ, EntityType.FIREWORK);

                    FireworkMeta meta = fw_pZX.getFireworkMeta();
                    FireworkEffect.Type type = FireworkEffect.Type.BALL_LARGE;
                    FireworkEffect eff = FireworkEffect.builder().flicker(true).withColor(this.winningTeamColor).with(type).trail(true).build();
                    meta.addEffect(eff);
                    meta.setPower(1);
                    fw_pZX.setFireworkMeta(meta);
                    fw_pXnZ.setFireworkMeta(meta);
                    fw_pZnX.setFireworkMeta(meta);
                    fw_nXZ.setFireworkMeta(meta);
                    detonateFirework(fw_pZX);
                    detonateFirework(fw_pXnZ);
                    detonateFirework(fw_pZnX);
                    detonateFirework(fw_nXZ);
                }
                for (Player p : Bukkit.getOnlinePlayers())
                    p.setGlowing(false);
                break;
        }
        updateDisconnect();
    }

    public void saveToFile() {
        FileConfiguration cfg = new YamlConfiguration();
        // Save general information
        cfg.set("preset", properties.name);
        try {
            cfg.save(new File(plugin.getDataFolder(), "arena.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendEveryoneToTeamChannels() {
        if (UHC.uhcNetwork == null)
            return;
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Bukkit.broadcastMessage(UtilChat.message("Creating discord channels..."));
            // Don't create a channel for lone wolves
            teamHandler.teams(false).stream().filter(team -> !(team instanceof LoneWolfTeam)).forEach(t -> new BotCommandNewTeam(plugin.serverId(), t.getTeamName()).publishBlocking());

            Bukkit.broadcastMessage(UtilChat.message("Moving everyone into their discord channels"));
            HashMap<UUID, String> teams = new HashMap<>();

            teamHandler.teams().forEach(t -> t.getPlayers().forEach(u -> teams.put(u, t.getTeamName())));
            new BotCommandAssignTeams(plugin.serverId(), null, teams).publishBlocking();

            ModuleRegistry.getLoadedModule(LoneWolfModule.class).ifPresent( loneWolfModule -> {
                for (UUID loneWolf : loneWolfModule.getLoneWolves()) {
                    plugin.getLogger().info("Assigning lone wolf to " + loneWolf.toString());
                    new BotCommandLoneWolf(plugin.serverId(), loneWolf, BotCommandLoneWolf.Command.ASSIGN).publishBlocking();
                }
            });

            Bukkit.broadcastMessage(UtilChat.message("Everyone should be moved"));
        });
    }

    public void setState(State state) {
        Bukkit.getServer().getPluginManager().callEvent(new GameStateChangeEvent(this.state, state));
        this.state = state;
    }

    public void spectate(Player player) {
        spectate(player, false);
    }

    public void spectate(Player player, boolean switchRole) {
        teamHandler.joinTeam(teamHandler.spectatorsTeam(), player);
        if (switchRole) {
            if (uhcNetwork != null) {
                new BotCommandAssignSpectator(plugin.serverId(), player.getUniqueId()).publish();
            }
        }
    }

    public void start() {
        GameListener.resetDeaths();

        getWorld().setGameRuleValue("naturalRegeneration", "false");
        getWorld().setGameRuleValue("doMobSpawning", "true");
        getWorld().setGameRuleValue("doMobLoot", "true");
        getWorld().setGameRuleValue("doDaylightCycle", "true");
        getWorld().setTime(0);

        PotionEffect resist = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 6, true, false);
        PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 30 * 20, 10, true, false);
        PotionEffect sat = new PotionEffect(PotionEffectType.SATURATION, 30 * 20, 20, true, false);

        for (Player p : players) {
            UtilTitle.title(p, null, ChatColor.GOLD + "The game has begun!", 10, 20 * 5, 20);
            p.sendMessage(UtilChat.message("The game has begun"));
            if (!teamHandler.isSpectator(p)) {
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
            }
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            if (!teamHandler.isSpectator(p)) {
                p.getInventory().clear();
                for (PotionEffect e : p.getActivePotionEffects()) {
                    p.removePotionEffect(e.getType());
                }
            } else
                new SpecInventory(plugin, p, teamHandler);
            p.setBedSpawnLocation(getCenter(), true);
            p.addPotionEffects(Arrays.asList(resist, regen, sat));
            if (p.isOp()) {
                p.setOp(false);
                previouslyOpped.add(p);
            }
            p.getInventory().clear();
            p.closeInventory();
        }


        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "xp -3000l @a");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "achievement take * @a");

        for (Entity e : getWorld().getEntities()) {
            if (e instanceof Tameable) {
                ((Tameable) e).setOwner(null);
                ((Tameable) e).setTamed(false);
            }
            if (e.getType() == EntityType.DROPPED_ITEM) {
                e.remove();
            }
        }


        sendEveryoneToTeamChannels();

        setState(RUNNING);
        startTime = System.currentTimeMillis();


    }

    public void startCountdown() {
        countdown = 10;
        setState(COUNTDOWN);

        teamHandler.teams().forEach(t -> scoreboardUpdater.getScoreboard().addTeam(t));
    }

    public void stop(String winner) {
        this.winner = winner;

        winner = WordUtils.capitalizeFully(winner.replace('_', ' '));


        getWorld().setGameRuleValue("doDaylightCycle", "false");
        getWorld().setGameRuleValue("doMobSpawning", "false");
        getWorld().setGameRuleValue("doMobLoot", "false");

        getWorld().setTime(1200);
        getWorld().setThundering(false);
        getWorld().setStorm(false);

        for (Player p : players) {
            UtilTitle.title(p, ChatColor.GOLD + winner, ChatColor.GOLD + "has won the game", 10, 20 * 5, 20);
            p.teleport(getCenter());
            p.setGameMode(GameMode.SURVIVAL);
            p.setAllowFlight(true);
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.getInventory().clear();
            p.setGlowing(false);
            p.setFlySpeed(0.1f);
            p.setWalkSpeed(0.2f);
            for (PotionEffect f : p.getActivePotionEffects()) {
                p.removePotionEffect(f.getType());
            }
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 2);
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 0.5f);
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1);
            if (teamHandler.isSpectator(p)) {
                for (Player olp : Bukkit.getOnlinePlayers()) {
                    olp.showPlayer(p);
                }
            }
        }

        for (Player p : previouslyOpped) {
            p.setOp(true);
        }

        bringEveryoneToLobby();

        MOTDHandler.setMotd(ChatColor.RESET + "" + ChatColor.RED + ChatColor.MAGIC + "|..|" + ChatColor.RESET + "  " + ChatColor.GOLD + winner + ChatColor.RED + " has won the game!  " + ChatColor.RED + ChatColor.MAGIC + "|..|");

        launchedFw = 0;

        setState(ENDGAME);
    }

    public int teamCountLeft() {
        return teamsLeft().size();
    }

    public ArrayList<UHCTeam> teamsLeft() {
        HashSet<UHCTeam> uniqueTeams = new HashSet<>();
        for (Player p : players) {
            if (queuedTeamRemovals.contains(p.getUniqueId()))
                continue;
            UHCTeam team = teamHandler.getTeamForPlayer(p);
            if (team == null)
                continue;
            if (team instanceof TeamSpectator)
                continue;
            uniqueTeams.add(team);
        }
        return new ArrayList<>(uniqueTeams);
    }

    //todo: Remove me
    public void temp_FireworkLaunch(Color color) {
        this.launchedFw = 0;
        this.winningTeamColor = color;
        setState(ENDGAME);
    }

    public void toggleShouldEndCheck() {
        this.properties.CHECK_ENDING.setValue(!this.properties.CHECK_ENDING.get());
        if (this.properties.CHECK_ENDING.get())
            Bukkit.broadcastMessage(UtilChat.message("Checking if the game should end"));
        else
            Bukkit.broadcastMessage(UtilChat.message("No longer checking if the game should end"));
    }


    public void unfreeze() {
        if (secondsRemaining > 0) {
            getWorld().getWorldBorder().setSize(overworldWorldborderSize);
            getWorld().getWorldBorder().setSize(properties.WORLDBORDER_END_SIZE.get(), secondsRemaining);
            if (getNether() != null) {
                getNether().getWorldBorder().setSize(netherWorldborderSize);
                getNether().getWorldBorder().setSize(properties.WORLDBORDER_END_SIZE.get() * 2, secondsRemaining);
            }
        }
        this.state = RUNNING;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!teamHandler.isSpectator(p)) {
                FreezeHandler.unfreeze(p);
            }
        }
        long frozenFor = System.currentTimeMillis() - freezeStartTime;
        System.out.println("Frozen for: " + frozenFor);
        this.startTime -= frozenFor;
        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
            FreezeHandler.restoreBlocks();
            FreezeHandler.pvpEnabled = true;
            Bukkit.broadcastMessage(UtilChat.message("Damage enabled"));
        }, 100);
        getWorld().setGameRuleValue("doDaylightCycle", "true");
        Bukkit.broadcastMessage(UtilChat.message("The game was frozen for " + ChatColor.GOLD + UtilTime.format(1, frozenFor, UtilTime.TimeUnit.FIT)));
        Bukkit.broadcastMessage(UtilChat.message("PvP will be enabled in 5 seconds"));
        FreezeHandler.frozenEntities.clear();
    }

    public void updateDisconnect() {
        Iterator<Map.Entry<UUID, Long>> iterator = logoutTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (System.currentTimeMillis() > entry.getValue()) {
                String playerName = uuidToStringMap.get(entry.getKey());
                Bukkit.broadcastMessage(generateBoldChat(playerName + " has been eliminated because they logged off 5 minutes ago!", net.md_5.bungee.api.ChatColor.WHITE).toLegacyText());
                queuedTeamRemovals.add(entry.getKey());
                iterator.remove();
                players.removeIf(next -> next.getUniqueId().equals(entry.getKey()));
                uuidToStringMap.remove(entry.getKey());
            }
        }
    }

    private void detonateFirework(Firework firework) {
        ((CraftWorld) firework.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity) firework).getHandle(), (byte) 17);
        firework.remove();
    }

    private void displayTip() {
        String tip = tips[new Random().nextInt(tips.length)];
        if (tip.contains("%NAME%")) {
            tip = tip.replace("%NAME%", randomPlayer());
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Tip> " + ChatColor.RESET + tip);
            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1F, 1F);
        }
        nextTipIn = System.currentTimeMillis() + TIP_TIME;
    }

    private boolean entityExists(UUID u, World world) {
        for (Entity e : world.getEntities()) {
            if (e.getUniqueId().equals(u)) {
                return true;
            }
        }
        return false;
    }

    private int getSpectatorCount() {
        int count = 0;
        for (Player p : players) {
            if (teamHandler.isSpectator(p))
                count++;
        }
        return count;
    }

    private int playerCount() {
        return this.players.size() - getSpectatorCount();
    }

    private String randomPlayer() {
        if (Bukkit.getOnlinePlayers().size() < 1) {
            return "PLAYERNOTFOUND";
        }
        return new ArrayList<>(Bukkit.getOnlinePlayers()).get(new Random().nextInt(Bukkit.getOnlinePlayers().size())).getName();
    }

    public enum State {
        INITIALIZED,
        GENERATING_WORLD,
        WAITING,
        COUNTDOWN,
        RUNNING,
        FROZEN,
        ENDGAME
    }

}
