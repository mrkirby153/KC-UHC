package me.mrkirby153.kcuhc.arena;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.gui.SpecInventory;
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
import me.mrkirby153.kcuhc.utils.UtilTitle;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandAssignSpectator;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandLoneWolf;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandToLobby;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandAssignTeams;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandNewTeam;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandRemoveTeam;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static me.mrkirby153.kcuhc.UHC.uhcNetwork;
import static me.mrkirby153.kcuhc.arena.UHCArena.State.*;

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

    private State state = State.INITIALIZED;

    private int generationTaskId;
    private int countdown;


    private int launchedFw = 0;
    private Color winningTeamColor = Color.WHITE;

    private List<Player> previouslyOpped = new ArrayList<>();

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

    private HashMap<UUID, BukkitTask> queuedRemovals = new HashMap<>();
    private HashSet<UUID> disconnectedPlayers = new HashSet<>();

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

    public void generate() {
        setState(State.GENERATING_WORLD);

        Double worldborderSize = properties.WORLDBORDER_START_SIZE.get() * (2D/3);
        // TODO: 12/27/2016 Worldborder size is diameter?
        int minX = (int) Math.ceil(getCenter().getBlockX() - worldborderSize);
        int maxX = (int) Math.ceil(getCenter().getBlockX() + worldborderSize);
        int minZ = (int) Math.ceil(getCenter().getBlockZ() - worldborderSize);
        int maxZ = (int) Math.ceil(getCenter().getBlockZ() + worldborderSize);


        plugin.getLogger().info(String.format("[PREGENERATION] Generating blocks from (%s, %s) to (%s, %s)", minX, minZ, maxX, maxZ));
        new GenerationTask(plugin, getWorld(), minX, maxX, minZ, maxZ);
    }

    public void generationComplete() {
        setState(State.WAITING);
        MOTDHandler.setMotd(ChatColor.GRAY + "Pending initialization");
        Bukkit.broadcastMessage(UtilChat.message("World generation complete"));
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1F, 1F));
        initialize();
    }

    public Location getCenter() {
        return getWorld().getHighestBlockAt((int) properties.WORLDBORDER_CENTER.get().getX(), (int) properties.WORLDBORDER_CENTER.get().getZ()).getLocation();
    }

    public World getNether() {
        return plugin.uhcWorld_nether;
    }

    public ArenaProperties getProperties() {
        return properties;
    }

    public void setProperties(ArenaProperties properties) {
        this.properties = properties;
    }

    public World getWorld() {
        return plugin.uhcWorld;
    }

    public void initialize() {
        getWorld().setGameRuleValue("doDaylightCycle", "false");
        getWorld().setGameRuleValue("doMobSpawning", "false");
        getWorld().setGameRuleValue("doMobLoot", "false");
        getWorld().setTime(1200);
        Bukkit.broadcastMessage(UtilChat.message("Initialized"));
        MOTDHandler.setMotd(ChatColor.GREEN + "Ready");
        launchedFw = 0;
        winningTeamColor = Color.WHITE;
        setState(INITIALIZED);
        queuedRemovals.forEach(((uuid, bukkitTask) -> bukkitTask.cancel()));
        queuedRemovals.clear();
        disconnectedPlayers.clear();
        previouslyOpped.clear();
    }

    public List<Player> players() {
        List<Player> players = new ArrayList<>();
        players.addAll(Bukkit.getOnlinePlayers());
        return players;
    }

    public List<Player> players(boolean spectators) {
        List<Player> p = new ArrayList<>(players());
        if (!spectators)
            p.removeIf(teamHandler::isSpectator);
        return p;
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
                    for (Player p : players()) {

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
                MOTDHandler.setMotd(ChatColor.RED + "Game in progress. " + ChatColor.AQUA + "" + (playerCount()) + ChatColor.RED + " alive");
                if (ModuleRegistry.isLoaded(WorldBorderModule.class)) {
                    Optional<WorldBorderModule> optWorldborderMod = ModuleRegistry.getLoadedModule(WorldBorderModule.class);
                    if (optWorldborderMod.isPresent()) {
                        WorldBorderModule worldBorderModule = optWorldborderMod.get();
                        worldBorderModule.setWarningDistance(0);
                        if (!notifiedDisabledSpawn && worldBorderModule.travelComplete()) {
                            for (Player p : Bukkit.getOnlinePlayers()) {
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
    }

    public void playerDisconnect(Player player) {
        Integer rejoinMinutes = getProperties().REJOIN_MINUTES.get();
        players().forEach(p -> p.spigot().sendMessage(C.formattedChat(player.getName() + " has disconnected " + rejoinMinutes + " minutes to rejoin", net.md_5.bungee.api.ChatColor.RED, C.Style.BOLD)));
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            players().forEach(p -> p.spigot().sendMessage(C.formattedChat(player.getName() + " been eliminated because they logged off " + rejoinMinutes + " ago", net.md_5.bungee.api.ChatColor.RED, C.Style.BOLD)));
            this.disconnectedPlayers.remove(player.getUniqueId());
            this.queuedRemovals.remove(player.getUniqueId());
        }, rejoinMinutes * 60 * 20);
        this.queuedRemovals.put(player.getUniqueId(), task);
        this.disconnectedPlayers.add(player.getUniqueId());
    }

    public void playerReconnect(Player player) {
        if (this.disconnectedPlayers.remove(player.getUniqueId())) {
            BukkitTask task = queuedRemovals.remove(player.getUniqueId());
            if (task == null) {
                spectate(player);
                return;
            }
            task.cancel();
            players().forEach(p -> p.spigot().sendMessage(C.formattedChat(player.getName() + " has reconnected!", net.md_5.bungee.api.ChatColor.GREEN, C.Style.BOLD)));
        } else {
            spectate(player);
        }
    }

    public List<OfflinePlayer> getDisconnectedPlayers() {
        ArrayList<OfflinePlayer> players = new ArrayList<>();
        players.addAll(this.disconnectedPlayers.stream().map(Bukkit::getOfflinePlayer).filter(Objects::nonNull).collect(Collectors.toList()));
        return players;
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

            ModuleRegistry.getLoadedModule(LoneWolfModule.class).ifPresent(loneWolfModule -> {
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

        for (Player p : players()) {
            UtilTitle.title(p, null, ChatColor.GOLD + "The game has begun!", 10, 20 * 5, 20);
            p.sendMessage(UtilChat.message("The game has begun"));
            if (!teamHandler.isSpectator(p)) {
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
            }
            p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setFoodLevel(20);
            if (!teamHandler.isSpectator(p)) {
                p.getInventory().clear();
                for (PotionEffect e : p.getActivePotionEffects()) {
                    p.removePotionEffect(e.getType());
                }
                p.setBedSpawnLocation(getCenter(), true);
                p.addPotionEffects(Arrays.asList(resist, regen, sat));
                p.getInventory().clear();
            } else {
                new SpecInventory(plugin, p, teamHandler);
            }
            if (p.isOp()) {
                p.setOp(false);
                previouslyOpped.add(p);
            }
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

    public void stop(String winner, Color winningTeamColor) {
        this.winner = winner;
        this.winningTeamColor = winningTeamColor;

        winner = WordUtils.capitalizeFully(winner.replace('_', ' '));


        getWorld().setGameRuleValue("doDaylightCycle", "false");
        getWorld().setGameRuleValue("doMobSpawning", "false");
        getWorld().setGameRuleValue("doMobLoot", "false");

        getWorld().setTime(1200);
        getWorld().setThundering(false);
        getWorld().setStorm(false);

        for (Player p : players()) {
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

    //todo: Remove me
    public void temp_FireworkLaunch(Color color) {
        this.launchedFw = 0;
        this.winningTeamColor = color;
        setState(ENDGAME);
    }


    private void detonateFirework(Firework firework) {
        PacketContainer container = new PacketContainer(PacketType.Play.Server.ENTITY_STATUS);
        container.getModifier().writeDefaults();
        container.getIntegers().write(0, firework.getEntityId());
        container.getBytes().write(0, (byte) 17);
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(container);

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

    private int playerCount() {
        return this.players(false).size() + this.disconnectedPlayers.size();
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
