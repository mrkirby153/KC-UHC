package me.mrkirby153.kcuhc.arena;


import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.UtilTime;
import me.mrkirby153.kcuhc.discord.commands.AssignSpectator;
import me.mrkirby153.kcuhc.discord.commands.AssignTeams;
import me.mrkirby153.kcuhc.discord.commands.ToLobby;
import me.mrkirby153.kcuhc.gui.SpecInventory;
import me.mrkirby153.kcuhc.handler.*;
import me.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.TeamSpectator;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_9_R2.*;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static me.mrkirby153.kcuhc.UHC.discordHandler;
import static me.mrkirby153.kcuhc.UtilChat.generateBoldChat;
import static me.mrkirby153.kcuhc.arena.UHCArena.EndgamePhase.NORMALGAME;
import static me.mrkirby153.kcuhc.arena.UHCArena.EndgamePhase.SHRINKING_WORLDBORDER;
import static me.mrkirby153.kcuhc.arena.UHCArena.State.*;

public class UHCArena implements Runnable, Listener {


    private ArrayList<Player> players = new ArrayList<>();

    private State state = State.INITIALIZED;
    private int generationTaskId;
    private PregameListener pregameListener = new PregameListener();
    private GameListener gameListener = new GameListener();
    private int countdown;
    private int deathmatch = -1;
    private boolean dmStarted = false;
    private boolean shouldEndCheck = true;
    private boolean shouldSpreadPlayers = true;
    public UHCScoreboard scoreboard;

    private String winner;

    private final World world;
    private final World nether;

    private final Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
    private final int startSize;
    private final int endSize;
    private final int duration;
    private final int minX, maxX, minZ, maxZ;
    private final Location center;

    private int percentToGlow = 15;
    private int startingPlayers;

    private WorldBorderHandler worldBorderHandler;

    private double barProgress = 1;
    private static final double BAR_PROGRESS_DEC = 0.1;
    private final int WORLDBORDER_WARN_DIST = 50;

    private double worldborderInitSize;

    private static final int FIREWORKS_TO_LAUNCH = 10;
    private int launchedFw = 0;

    private Color winningTeamColor = Color.WHITE;

    private List<Player> previouslyOpped = new ArrayList<>();

    private HashMap<UUID, String> uuidToStringMap = new HashMap<>();
    private HashMap<UUID, Long> logoutTimes = new HashMap<>();
    private ArrayList<UUID> queuedTeamRemovals = new ArrayList<>();

    private long startTime = 0;

    private long nextEndgamePhaseIn = -1;
    private EndgamePhase currentEndgamePhase = EndgamePhase.NORMALGAME;
    private EndgamePhase nextEndgamePhase;

    private int secondsRemaining;
    private double overworldWorldborderSize;
    private double netherWorldborderSize;
    private long freezeStartTime;
    private EndgamePhase frozen_endgamePhase;
    private EndgamePhase frozen_nextEndgamePhase;
    private long frozen_nextEndgamePhaseIn;

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
            ChatColor.GOLD + "In the event of an emergency, do not panic! Use /a <message> to contact an admin!"

    };

    private long nextTipIn = -1;
    private static final int TIP_TIME = 30000;


    public UHCArena(World world, int startSize, int endSize, int duration, Location center) {
        this.world = world;
        this.nether = Bukkit.getWorld(world.getName() + "_nether");
        this.startSize = startSize;
        this.endSize = endSize;
        this.duration = duration;
        this.center = world.getHighestBlockAt(center.getBlockX(), center.getBlockZ()).getLocation();
        this.worldBorderHandler = new WorldBorderHandler(UHC.plugin, this, world, nether);
        minX = center.getBlockX() - startSize;
        maxX = center.getBlockX() + startSize;
        minZ = center.getBlockZ() - startSize;
        maxZ = center.getBlockZ() + startSize;
        UHC.plugin.getServer().getPluginManager().registerEvents(pregameListener, UHC.plugin);
        UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, this, 0, 20);
        UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, this::drawScoreboard, 0, 1L);
        Thread endgameThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    updateEndgame();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        endgameThread.setName("Endgame Thread");
        endgameThread.setDaemon(true);
        endgameThread.start();
        UHC.plugin.getServer().getPluginManager().registerEvents(this, UHC.plugin);
        this.scoreboard = new UHCScoreboard();
        craftingRecipes();
    }

    public void generate() {
        MOTDHandler.setMotd(ChatColor.DARK_RED + "Pregenerating world, come back soon!");
        state = State.GENERATING_WORLD;
        generationTaskId = UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin,
                new GenerationTask(UHC.plugin, this, world, minX / 512, minZ / 512, maxX / 512, maxZ / 512), 1L, 1L);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer(net.md_5.bungee.api.ChatColor.RED + "We are pregenerating the world, come back later");
        }
    }

    public void generationComplete() {
        state = State.WAITING;
        MOTDHandler.setMotd(ChatColor.GRAY + "Pending initialization");
        Bukkit.broadcastMessage(UtilChat.message("World generation complete"));
        UHC.plugin.getServer().getScheduler().cancelTask(generationTaskId);
        initialize();
    }

    //todo: Remove me
    public void temp_FireworkLaunch(Color color) {
        this.launchedFw = 0;
        this.winningTeamColor = color;
        this.state = ENDGAME;
    }

    public void initialize() {
        currentEndgamePhase = EndgamePhase.NORMALGAME;
        nextEndgamePhase = null;
        nextEndgamePhaseIn = -1;
        scoreboard.createTeams();
        this.worldBorderHandler.setWorldborder(60);
        this.worldBorderHandler.setWarningDistance(0);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doMobLoot", "false");
        world.setTime(1200);
        Bukkit.broadcastMessage(UtilChat.message("Initialized"));
        MOTDHandler.setMotd(ChatColor.GREEN + "Ready");
        players = new ArrayList<>();
        dmStarted = false;
        launchedFw = 0;
        winningTeamColor = Color.WHITE;
        state = State.INITIALIZED;
        queuedTeamRemovals.clear();
        uuidToStringMap.clear();
        logoutTimes.clear();
        players.clear();
        previouslyOpped.clear();
    }

    public void start() {
        GameListener.resetDeaths();
        currentEndgamePhase = EndgamePhase.NORMALGAME;
        nextEndgamePhase = null;
        nextEndgamePhaseIn = -1;
        this.worldBorderHandler.setWorldborder(startSize);
        this.worldBorderHandler.setWarningDistance(WORLDBORDER_WARN_DIST);
        this.worldBorderHandler.setWorldborder(endSize, duration);
        HandlerList.unregisterAll(pregameListener);
        UHC.plugin.getServer().getPluginManager().registerEvents(gameListener, UHC.plugin);
        world.setGameRuleValue("naturalRegeneration", "false");
        world.setGameRuleValue("doMobSpawning", "true");
        world.setGameRuleValue("doMobLoot", "true");
        world.setGameRuleValue("doDaylightCycle", "true");
        PotionEffect resist = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 6, true, false);
        PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 30 * 20, 10, true, false);
        PotionEffect sat = new PotionEffect(PotionEffectType.SATURATION, 30 * 20, 20, true, false);
        for (Player p : players) {
            PacketPlayOutTitle timings = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, 10, 20 * 5, 20);
            PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, IChatBaseComponent.ChatSerializer.a("{\"text\":\"\"}"));
            PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                    IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.GOLD + "The game has begun!")));
            p.sendMessage(UtilChat.message("The game has begun"));
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(timings);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(subtitle);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(title);
            if (!TeamHandler.isSpectator(p)) {
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
            }
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            if (!TeamHandler.isSpectator(p)) {
                p.getInventory().clear();
                for (PotionEffect e : p.getActivePotionEffects()) {
                    p.removePotionEffect(e.getType());
                }
            } else
                new SpecInventory(UHC.plugin, p);
            p.setBedSpawnLocation(center, true);
            p.addPotionEffects(Arrays.asList(resist, regen, sat));
            if (p.isOp()) {
                p.setOp(false);
                previouslyOpped.add(p);
            }
            p.getInventory().clear();
            p.closeInventory();
        }
        if (shouldSpreadPlayers)
            distributeTeams(50);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "xp -3000l @a");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "achievement take * @a");
        for (Entity e : world.getEntities()) {
            if (e instanceof Tameable) {
                ((Tameable) e).setOwner(null);
                ((Tameable) e).setTamed(false);
            }
            if (e.getType() == EntityType.DROPPED_ITEM) {
                e.remove();
            }
        }
        for (Player p : players()) {
            RegenTicket.give(p);
            UHC.playerTracker.giveTracker(p);
        }
        sendEveryoneToTeamChannels();
        startingPlayers = players.size() - getSpectatorCount();
        state = State.RUNNING;
        startTime = System.currentTimeMillis();
        if (UHC.plugin.getConfig().getBoolean("episodes.use"))
            UHC.markerHandler.startTracking();
    }

    public void spectate(Player player) {
        spectate(player, false);
    }

    public void spectate(Player player, boolean switchRole) {
        TeamHandler.joinTeam(TeamHandler.spectatorsTeam(), player);
        if (switchRole) {
            if (discordHandler != null) {
                new AssignSpectator(player).send();
            }
        }
    }

    public void stop(String winner) {
        String orig = winner;
        this.winner = winner;
        this.nextEndgamePhaseIn = -1;
        this.nextEndgamePhase = null;
        RegenTicket.clearRegenTickets();
        winner = WordUtils.capitalizeFully(winner.replace('_', ' '));
        this.worldBorderHandler.setWorldborder(60);
        this.worldBorderHandler.setWarningDistance(0);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doMobLoot", "false");
        world.setTime(1200);
        world.setThundering(false);
        world.setStorm(false);
        for (Player p : players) {
            PacketPlayOutTitle timings = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, 10, 20 * 5, 20);
            PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE,
                    IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.GOLD + winner)));
            PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                    IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.GREEN + "has won the game!")));
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(timings);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(title);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(subtitle);
            p.teleport(center);
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
            if (TeamHandler.isSpectator(p)) {
                for (Player olp : Bukkit.getOnlinePlayers()) {
                    olp.showPlayer(p);
                }
            }
        }
        for (Player p : previouslyOpped) {
            p.setOp(true);
        }
        HandlerList.unregisterAll(gameListener);
        UHC.plugin.getServer().getPluginManager().registerEvents(pregameListener, UHC.plugin);
        bringEveryoneToLobby();
        MOTDHandler.setMotd(ChatColor.RESET + "" + ChatColor.RED + ChatColor.MAGIC + "|..|" + ChatColor.RESET + "  " + ChatColor.GOLD + winner + ChatColor.RED + " has won the game!  " + ChatColor.RED + ChatColor.MAGIC + "|..|");
        launchedFw = 0;
        UHC.markerHandler.stopTracking();
        state = ENDGAME;
    }

    public void startCountdown() {
        countdown = 10;
        barProgress = 1;
        state = COUNTDOWN;
    }

    public void essentiallyDisable() {
        if (UHC.discordHandler != null)
            UHC.discordHandler.deleteAllTeamChannels(null);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setAllowFlight(false);
            p.setGameMode(GameMode.SURVIVAL);
            p.teleport(p.getLocation().add(0, 10, 0));
        }
        HandlerList.unregisterAll(UHC.plugin);
        Bukkit.getServer().getScheduler().cancelTasks(UHC.plugin);
        this.worldBorderHandler.setWorldborder(this.worldBorderHandler.getOverworld().getSize() + 150, 0);
        Objective healthObj = board.getObjective("health");
        if (healthObj != null)
            healthObj.unregister();
        UHC.plugin.getPluginLoader().disablePlugin(UHC.plugin);
    }

    public State currentState() {
        return this.state;
    }

    public ArrayList<UHCPlayerTeam> teamsLeft() {
        ArrayList<UHCPlayerTeam> uniqueTeams = new ArrayList<>();
        for (Player p : players) {
/*            System.out.println("//////////////////////");
            System.out.println(queuedTeamRemovals.toString());
            System.out.println(logoutTimes.keySet().toString());*/
            if (queuedTeamRemovals.contains(p.getUniqueId()))
                continue;
            UHCTeam team = TeamHandler.getTeamForPlayer(p);
            if (!(team instanceof UHCPlayerTeam))
                continue;
            UHCPlayerTeam pTeam = (UHCPlayerTeam) team;
            if (pTeam == TeamHandler.getTeamByName(TeamHandler.SPECTATORS_TEAM))
                continue;
            if (!uniqueTeams.contains(pTeam)) {
                uniqueTeams.add(pTeam);
            }
        }
        return uniqueTeams;
    }

    public int teamCountLeft() {
        return teamsLeft().size();
    }

    public void newTeam(String name, net.md_5.bungee.api.ChatColor color) {
        TeamHandler.registerTeam(name, new UHCPlayerTeam(name, color));
    }

    public void handleDeathMessage(Player dead, String message) {
        for (Player p : players) {
            PacketPlayOutTitle timings = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, 10, 20 * 5, 20);
            PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE,
                    IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.DARK_PURPLE + dead.getDisplayName())));
            PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                    IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.AQUA + message.replace(dead.getName(), ""))));
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(timings);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(title);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(subtitle);
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 1, 1);
        }
        Location playerLoc = dead.getLocation();
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta m = head.getItemMeta();
        ((SkullMeta) m).setOwner(dead.getName());
        head.setItemMeta(m);
        playerLoc.getWorld().dropItemNaturally(playerLoc, head);
        players.remove(dead);
        dead.sendMessage(UtilChat.message("You have died and are now a spectator"));
    }

    public void saveToFile() {
        FileConfiguration cfg = new YamlConfiguration();
        // Save general information
        cfg.set("duration", duration);
        cfg.set("endSize", endSize);
        cfg.set("startSize", startSize);
        cfg.set("world", world.getName());
        cfg.set("startingPoint", center);
        cfg.set("glowPercent", percentToGlow);
        try {
            cfg.save(new File(UHC.plugin.getDataFolder(), "arena.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static UHCArena loadFromFile() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(UHC.plugin.getDataFolder(), "arena.yml"));
        UHCArena uhcArena = new UHCArena(Bukkit.getWorld(cfg.getString("world")), cfg.getInt("startSize"), cfg.getInt("endSize"), cfg.getInt("duration"), (Location) cfg.get("startingPoint"));
        uhcArena.setPercentToGlow(cfg.getInt("glowPercent"));
        return uhcArena;
    }

    CountdownBarTask countdownTask;

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
                    countdownTask = new CountdownBarTask(System.currentTimeMillis() + 10000, 10000);
                    countdownTask.setTaskId(UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, countdownTask, 0L, 1L));
                }
                if (countdown > 0) {
                    for (Player p : players) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1, 1);
                        PacketPlayOutTitle timings = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, 0, 21, 0);
                        PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE,
                                IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.GREEN + "Starting in")));
                        PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                                IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.RED + "" + countdown)));
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(timings);
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(title);
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(subtitle);
                        p.sendMessage(UtilChat.message("Starting in " + ChatColor.GOLD + countdown));
                    }
                    MOTDHandler.setMotd(ChatColor.YELLOW + "Starting in " + countdown);
                    countdown--;
                    barProgress -= BAR_PROGRESS_DEC;
                } else {
                    start();
                }
                break;
            case RUNNING:
                MOTDHandler.setMotd(ChatColor.RED + "Game in progress. " + ChatColor.AQUA + "" + (players.size() - getSpectatorCount()) + ChatColor.RED + " alive");
                for (Player p : players) {
                    p.setGlowing(false);
                }
                if (teamCountLeft() <= 1 && shouldEndCheck) {
                    if (teamsLeft().size() > 0) {
                        UHCPlayerTeam team = teamsLeft().get(0);
                        this.winningTeamColor = team.toColor();
                        stop(team.getFriendlyName());
                    }
                    state = ENDGAME;
                }
                if (worldBorderHandler.overworldTravelComplete()) {
                    if (!notifiedDisabledSpawn) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F);
                            p.sendMessage(UtilChat.message("Natural mob spawning has been cut by 75%"));
                        }

                        notifiedDisabledSpawn = true;
                    }
                    world.getWorldBorder().setWarningDistance(0);
                }
                if (nether != null)
                    if (worldBorderHandler.netherTravelComplete())
                        nether.getWorldBorder().setWarningDistance(0);
                if (deathmatch != -1) {
                    deathmatch--;
                    if (deathmatch > 0) {
                        for (Player p : players) {
                            PacketPlayOutTitle timings = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, 0, 21, 0);
                            PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE,
                                    IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", "")));
                            PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                                    IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.RED + "Deathmatch in " + ChatColor.GREEN + deathmatch)));
                            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(timings);
                            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(title);
                            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(subtitle);
                            if (deathmatch <= 3) {
                                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                            }
                        }
                    } else {
                        deathmatch();
                    }
                }
                break;
            case ENDGAME:
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setAllowFlight(true);
                    p.getInventory().clear();
                    TeamHandler.leaveTeam(p);
                }
                if (this.launchedFw++ < 8) {
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
        drawScoreboard();
    }

    private void detonateFirework(Firework firework) {
        ((CraftWorld) firework.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity) firework).getHandle(), (byte) 17);
        firework.remove();
    }

    private int runCount = 0;

    private void updateEndgame() {
        if (state != RUNNING)
            return;
        switch (currentEndgamePhase) {
            case NORMALGAME:
                if (world.getWorldBorder().getSize() <= endSize) {
                    if (nextEndgamePhase != EndgamePhase.SHRINKING_WORLDBORDER) {
                        nextEndgamePhaseIn = System.currentTimeMillis() + EndgamePhase.SHRINKING_WORLDBORDER.getDuration();
                        nextEndgamePhase = EndgamePhase.SHRINKING_WORLDBORDER;
                        firstAnnounce = true;
                    }
                }
                break;
            case SHRINKING_WORLDBORDER:
                runCount++;
                if (runCount % 40 == 0) {
                    runCount = 0;
                    WorldBorder wb = world.getWorldBorder();
                    if (wb.getSize() > 1)
                        wb.setSize(wb.getSize() - 2, 1);
                }
                nextEndgamePhase = null;
                break;
        }
        if (nextEndgamePhase != null && currentEndgamePhase != SHRINKING_WORLDBORDER)
            announcePhase(nextEndgamePhase);
        if (currentEndgamePhase != SHRINKING_WORLDBORDER)
            activatePhase();
    }


    boolean firstAnnounce = true;
    boolean announced = false;
    boolean shouldAnnounce = false;
    String lastAnnounced = "-1";

    private void announcePhase(EndgamePhase phase) {
        long time = nextEndgamePhaseIn - System.currentTimeMillis();
        double nextPhase = UtilTime.trim(1, time / 1000D);
        String tFormat = UtilTime.format(1, time, UtilTime.TimeUnit.FIT);
        if (nextPhase > 0) {
            if (nextPhase >= 60) {
                shouldAnnounce = (nextPhase % 60) == 0;
            } else if (nextPhase >= 30) {
                shouldAnnounce = (nextPhase % 30) == 0;
            } else if (nextPhase <= 10) {
                shouldAnnounce = (nextPhase % 1) == 0;
            }
        }
        if ((shouldAnnounce || firstAnnounce) && !lastAnnounced.equalsIgnoreCase(tFormat)) {
            lastAnnounced = Double.toString(nextPhase);
            shouldAnnounce = false;
            firstAnnounce = false;
            announced = true;
            lastAnnounced = tFormat;
            BaseComponent text = generateBoldChat(phase.getName(), net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);
            text.addExtra(UtilChat.generateBoldChat(" will be applied in " + tFormat, net.md_5.bungee.api.ChatColor.GREEN));
            for (Player p : players()) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F);
                p.spigot().sendMessage(text);
            }
        }
    }

    private void activatePhase() {
        if (nextEndgamePhaseIn != -1 && System.currentTimeMillis() > nextEndgamePhaseIn) {
            activatePhase(nextEndgamePhase);
        }
    }

    private void activatePhase(EndgamePhase newPhase) {
        if (newPhase == null)
            return;
        this.currentEndgamePhase = newPhase;
        firstAnnounce = true;
        BaseComponent text = generateBoldChat(newPhase.getName(), net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);
        text.addExtra(UtilChat.generateBoldChat(" active!", net.md_5.bungee.api.ChatColor.DARK_RED));
        for (Player p : players()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1F, 1F);
            p.spigot().sendMessage(text);
        }
    }


    private void drawScoreboard() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboard() != scoreboard.getBoard())
                p.setScoreboard(scoreboard.getBoard());
        }
        scoreboard.reset();
        switch (currentState()) {
            case WAITING:
            case INITIALIZED:
                scoreboard.add(" ");
                scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "WAITING...");
                scoreboard.add(" ");
                scoreboard.add("Players: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
                scoreboard.add(" ");
                break;
            case FROZEN:
                scoreboard.add(ChatColor.RED + "" + ChatColor.GOLD + "The game is frozen!");
                break;
            case RUNNING:
                List<UUID> players = this.players.stream().map(Entity::getUniqueId).collect(Collectors.toList());
                players.removeAll(TeamHandler.spectatorsTeam().getPlayers());
                players.sort((o1, o2) -> {
                    Player p1 = Bukkit.getPlayer(o1);
                    Player p2 = Bukkit.getPlayer(o2);
                    if (p1 == null) {
                        return 1;
                    }
                    if (p2 == null) {
                        return -1;
                    }
                    UHCTeam team1 = TeamHandler.getTeamForPlayer(p1);
                    UHCTeam team2 = TeamHandler.getTeamForPlayer(p2);
                    if (team1 == null || TeamHandler.spectatorsTeam() == team1) {
                        return -1;
                    }
                    if (team2 == null || TeamHandler.spectatorsTeam() == team2) {
                        return 1;
                    }
                    if (team1.getName().equalsIgnoreCase(team2.getName())) {
                        return (int) Math.floor(p2.getHealth() - p1.getHealth());
                    } else {
                        return team1.getName().compareToIgnoreCase(team2.getName());
                    }
                });
                int spacesNeeded = players.size();
                if (spacesNeeded < ((nextEndgamePhaseIn == -1) ? 9 : 8)) {
                    for (UUID u : players) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(u);
                        Player onlinePlayer = null;
                        UHCTeam team;
                        if (op instanceof Player) {
                            team = TeamHandler.getTeamForPlayer((Player) op);
                            onlinePlayer = (Player) op;
                        } else
                            team = null;
                        if (team == null) {
                            scoreboard.add(ChatColor.GRAY + op.getName());
                        } else {
                            if (team instanceof TeamSpectator)
                                continue;
                            scoreboard.add(ChatColor.RED + "" + (int) onlinePlayer.getHealth() + " " + team.getColor() + op.getName());
                        }
                    }
                } else {
                    List<UHCTeam> teams = TeamHandler.teams().stream().filter(t -> t != TeamHandler.spectatorsTeam()).collect(Collectors.toList());
                    int teamsIngame = 0;
                    for (UHCTeam t : teams) {
                        if (t instanceof TeamSpectator)
                            continue;
                        if (t.getPlayers().stream().map(Bukkit::getPlayer).filter(p -> p != null).count() > 0)
                            teamsIngame++;
                    }
                    scoreboard.add(ChatColor.AQUA + "Teams: ");
                    if (teamsIngame > 9) {
                        scoreboard.add(ChatColor.GOLD + "" + teamsIngame + ChatColor.WHITE + " alive");
                    } else {
                        HashMap<String, Integer> onlineCount = new HashMap<>();
                        int offlineCount = 0;
                        for (UUID u : players) {
                            Player player = Bukkit.getPlayer(u);
                            if (player != null) {
                                UHCTeam team = TeamHandler.getTeamForPlayer(player);
                                if (team instanceof TeamSpectator)
                                    continue;
                                Integer i = onlineCount.get(team.getName());
                                if (i == null)
                                    i = 1;
                                else
                                    i++;
                                onlineCount.put(team.getName(), i);
                            } else {
                                offlineCount++;
                            }
                        }
                        for (String t : onlineCount.keySet()) {
                            scoreboard.add(onlineCount.get(t) + " " + TeamHandler.getTeamByName(t).getColor() + TeamHandler.getTeamByName(t).getFriendlyName());
                        }
                        if (offlineCount > 0)
                            scoreboard.add(offlineCount + "" + ChatColor.GRAY + " Offline");
                    }
                }
                scoreboard.add(" ");
                if (nextEndgamePhase == null && (currentEndgamePhase == NORMALGAME || currentEndgamePhase == SHRINKING_WORLDBORDER)) {
                    scoreboard.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "Worldborder:");
                    double[] wbPos = worldborderLoc();
                    scoreboard.add("from -" + UtilTime.trim(1, wbPos[0]) + " to +" + UtilTime.trim(1, wbPos[0]));
                    scoreboard.add(" ");
                } else if (nextEndgamePhase != null && nextEndgamePhaseIn != -1) {
                    scoreboard.add(ChatColor.DARK_RED + "" + ChatColor.BOLD + nextEndgamePhase.getName());
                    if (System.currentTimeMillis() > nextEndgamePhaseIn) {
                        scoreboard.add(" ACTIVE");
                    } else {
                        int trim = (int) (nextEndgamePhaseIn - System.currentTimeMillis());
                        scoreboard.add("  in " + UtilTime.format(1, trim, UtilTime.TimeUnit.FIT));
                    }
                    scoreboard.add(" ");
                }
                scoreboard.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Time Elapsed");
                scoreboard.add("  " + UtilTime.format(1, System.currentTimeMillis() - startTime, UtilTime.TimeUnit.FIT));
                break;
            case ENDGAME:
                scoreboard.add(" ");
                scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "ENDED!");
                scoreboard.add(" ");
                scoreboard.add(ChatColor.GOLD + "WINNER:");
                scoreboard.add("   " + winner);
                scoreboard.add(" ");
        }
        scoreboard.draw();
    }

    private void craftingRecipes() {
        ShapedRecipe headApple = new ShapedRecipe(new ItemStack(Material.GOLDEN_APPLE, 1));
        headApple.shape("GGG", "GHG", "GGG");
        headApple.setIngredient('G', Material.GOLD_INGOT);
        headApple.setIngredient('H', new MaterialData(Material.SKULL_ITEM, (byte) 3));
        Bukkit.getServer().addRecipe(headApple);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void craftGoldenHead(PrepareItemCraftEvent event) {
        if (event.getRecipe().getResult() == null)
            return;
        Material type = event.getRecipe().getResult().getType();
        if (type != Material.GOLDEN_APPLE)
            return;
        if (event.getInventory() == null)
            return;
        CraftingInventory inv = event.getInventory();
        for (ItemStack item : inv.getMatrix()) {
            if (item != null && item.getType() != Material.AIR) {
                if (item.getType() == Material.SKULL_ITEM || item.getType() == Material.SKULL) {
                    if (item.getItemMeta() == null)
                        continue;
                    ItemStack apple = new ItemStack(Material.GOLDEN_APPLE, 1);
                    ItemMeta meta = apple.getItemMeta();
                    meta.setDisplayName(ChatColor.AQUA + "Head Apple");
                    apple.setItemMeta(meta);
//                    apple.addEnchantment(new NullEnchantment(), 1);
                    apple.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
                    inv.setResult(apple);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void chatEvent(AsyncPlayerChatEvent event) {
        UHCTeam team = TeamHandler.getTeamForPlayer(event.getPlayer());
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void itemPickup(PlayerPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.SKULL_ITEM && event.getItem().getItemStack().getDurability() == 3) {
            Player player = event.getPlayer();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 0.5F);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ChatColor.BOLD + "=============================================");
            player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "You've picked up a player head!");
            player.sendMessage(ChatColor.WHITE + "You can use this head to craft a Golden Head for healing");
            player.sendMessage(ChatColor.WHITE + "A golden head will give you 2x the effects of a golden apple!");
            player.sendRawMessage(ChatColor.GREEN + "To Craft: " + ChatColor.WHITE + "Use the recipe for a Golden Apple, but replace the apple with the head");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "Optionally, right click the player head to eat it");
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ChatColor.BOLD + "=============================================");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void eatHead(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (event.getItem() != null)
                if (event.getItem().getType() == Material.SKULL_ITEM && event.getItem().getDurability() == 3) {
                    event.setCancelled(true);
                    if (event.getItem().getAmount() == 1)
                        event.getPlayer().getInventory().remove(event.getItem());
                    else {
                        event.getItem().setAmount(event.getItem().getAmount() - 1);
                        event.getPlayer().getInventory().setItemInMainHand(event.getItem());
                    }
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(event.getPlayer().getLocation(), Sound.ENTITY_PLAYER_BURP, 1F, 1F);
                    }
                    event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 125, 1));
                    event.getPlayer().sendMessage(UtilChat.message("You have been given Regeneration"));
                }
        }
    }

    @EventHandler
    public void consumeHeadApple(PlayerItemConsumeEvent event) {
        if (event.getItem().getItemMeta().getDisplayName() == null)
            return;
        if (!event.getItem().getItemMeta().getDisplayName().contains("Head"))
            return;
        if (!event.getItem().getEnchantments().containsKey(Enchantment.ARROW_DAMAGE))
            return;
        if (event.getItem().getType() != Material.GOLDEN_APPLE) {
            return;
        }
        event.getPlayer().sendMessage(UtilChat.message("You ate a head apple"));
        UHCTeam team = TeamHandler.getTeamForPlayer(event.getPlayer());
        if (team != null) {
            for (UUID u : team.getPlayers()) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));
                    if (p.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                        p.sendMessage(UtilChat.message("You have given your team Regeneration II and Absorption!"));
                    } else {
                        p.sendMessage(UtilChat.message(ChatColor.GOLD + event.getPlayer().getName() + ChatColor.GRAY + " ate a head apple, giving you Regeneration II and Absorption!"));
                    }
                }
            }
        } else {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));
            event.getPlayer().sendMessage(UtilChat.message("You are not on a team so only you get the effects"));
        }
    }


    private boolean entityExists(UUID u, World world) {
        for (Entity e : world.getEntities()) {
            if (e.getUniqueId().equals(u)) {
                return true;
            }
        }
        return false;
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

    private String randomPlayer() {
        if (Bukkit.getOnlinePlayers().size() < 1) {
            return "PLAYERNOTFOUND";
        }
        return new ArrayList<>(Bukkit.getOnlinePlayers()).get(new Random().nextInt(Bukkit.getOnlinePlayers().size())).getName();
    }

    private void deathmatch() {
        deathmatch = -1;
        // Teleport all players to the surface
        PotionEffect effect = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 5 * 20, 6, true, false);
        for (Player p : players) {
            if (TeamHandler.isSpectator(p))
                continue;
            p.addPotionEffect(effect);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1, 1);
            p.spigot().sendMessage(UtilChat.generateFormattedChat("Deathmatch has begun!", net.md_5.bungee.api.ChatColor.RED, 8));
        }
        dmStarted = true;
    }

    private int playerCount() {
        return this.players.size() - getSpectatorCount();
    }

    private int getSpectatorCount() {
        int count = 0;
        for (Player p : players) {
            if (TeamHandler.isSpectator(p))
                count++;
        }
        return count;
    }

    private double[] worldborderLoc() {
        WorldBorder wb = world.getWorldBorder();
        Location l = wb.getCenter();
        double locX = (wb.getSize() / 2) + l.getX();
        double locZ = (wb.getSize() / 2) + l.getZ();
        return new double[]{locX, locZ};
    }

    public void addPlayer(Player player) {
        Iterator<Player> p = players.iterator();
        while (p.hasNext()) {
            if (p.next().getUniqueId().equals(player.getUniqueId()))
                p.remove(); // Remove old player object
        }
        this.players.add(player);
    }

    public void setState(State state) {
        this.state = state;
    }

    public void toggleShouldEndCheck() {
        this.shouldEndCheck = !this.shouldEndCheck;
        if (shouldEndCheck)
            Bukkit.broadcastMessage(UtilChat.message("Checking if the game should end"));
        else
            Bukkit.broadcastMessage(UtilChat.message("No longer checking if the game should end"));
    }

    public void toggleSpreadingPlayers() {
        this.shouldSpreadPlayers = !this.shouldSpreadPlayers;
        if (shouldSpreadPlayers) {
            Bukkit.broadcastMessage(UtilChat.message("Spreading players once the game starts"));
        } else {
            Bukkit.broadcastMessage(UtilChat.message("No longer spreading players"));
        }
    }

    public void setPercentToGlow(int percent) {
        this.percentToGlow = percent;
    }

    public Location getCenter() {
        return center;
    }

    public Player[] players() {
        return players.toArray(new Player[players.size()]);
    }

    public void playerDisconnect(Player player) {
        UHCTeam teamForPlayer = TeamHandler.getTeamForPlayer(player);
        if (teamForPlayer == null || teamForPlayer instanceof TeamSpectator) {
            players.remove(player);
            return;
        }
        uuidToStringMap.put(player.getUniqueId(), player.getName());
//        players.remove(player);
        logoutTimes.put(player.getUniqueId(), System.currentTimeMillis() + (300000));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + player.getName() + " has disconnected! 5.0 minutes to rejoin!");
        }
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
                Iterator<Player> uhcPlayerIterator = players.iterator();
                while (uhcPlayerIterator.hasNext()) {
                    Player next = uhcPlayerIterator.next();
                    if (next.getUniqueId().equals(entry.getKey())) {
                        uhcPlayerIterator.remove();
                    }
                }
                uuidToStringMap.remove(entry.getKey());
            }
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
            TeamHandler.leaveTeam(player);
            players.remove(player);
            spectate(player);
            player.sendMessage(UtilChat.message("You have disconnected more than five minutes ago and have been removed from the game"));
        }
    }

    public void startDeathmatch() {
        deathmatch = 30;
        for (Player p : players) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_AMBIENT, 1, 1);
            p.spigot().sendMessage(generateBoldChat("When deathmatch starts, the world border will shrink to 1 block over the course of 5 minutes. " +
                    "Good luck!", net.md_5.bungee.api.ChatColor.GREEN));
        }
    }

    public World getWorld() {
        return world;
    }

    public int endSize() {
        return endSize;
    }


    public void sendEveryoneToTeamChannels() {
        if (discordHandler == null)
            return;
        Bukkit.broadcastMessage(UtilChat.message("Creating discord channels..."));
        UHC.discordHandler.createAllTeamChannels(() -> UHC.discordHandler.processAsync(() -> {
            Bukkit.broadcastMessage(UtilChat.message("Moving everyone to their discord channel"));
/*            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(UHC.plugin.serverId());
            out.writeUTF("assignTeams");
            out.writeInt((int) players.stream().filter(p -> TeamHandler.getTeamForPlayer(p) != null).count());
            players.stream().filter(p -> TeamHandler.getTeamForPlayer(p) != null).forEach(p -> {
                out.writeUTF(p.getUniqueId().toString());
                out.writeUTF(TeamHandler.getTeamForPlayer(p).getName());
            });
            UHC.discordHandler.sendMessage(out.toByteArray());*/
            new AssignTeams(TeamHandler.teams()).send();
        }, () -> Bukkit.broadcastMessage(UtilChat.message("Everyone should be moved"))));
    }

    public void bringEveryoneToLobby() {
        if (discordHandler == null)
            return;
        new ToLobby().send();
        UHC.discordHandler.deleteAllTeamChannels(null);
    }

    public void setEndgamePhase(EndgamePhase endgamePhase) {
        this.currentEndgamePhase = endgamePhase;
    }


    public void distributeTeams(int minRadius) {
        System.out.println("Worldborder location: +-" + world.getWorldBorder().getSize() / 2);
        System.out.println("Spreading teams...");
        Map<UHCTeam, Location> locations = new HashMap<>();
        for (UHCTeam team : TeamHandler.teams().stream().filter(t -> !(t instanceof TeamSpectator)).collect(Collectors.toList())) {
            Location randomSpawn = SpawnUtils.getRandomSpawn(world, startSize);
            locations.put(team, randomSpawn);
        }
        // Verify that the teams are spread far enough apart
        for (UHCTeam team : locations.keySet()) {
            Location spawnLoc = locations.get(team);
            // Attempt 1,000 tries
            for (int i = 0; i < 1000; i++) {
                boolean clash = false;
                for (Location oL : locations.values()) {
                    if (oL.distanceSquared(spawnLoc) < Math.pow(minRadius, 2) && !oL.equals(spawnLoc)) {
                        System.out.println("CLASH! " + spawnLoc.toString() + " is too close to " + oL.toString());
                        clash = true;
                    }
                    if (!clash)
                        break;
                    spawnLoc = SpawnUtils.getRandomSpawn(world, startSize);
                }
            }
            // Teleport everyone on the team to that location
            System.out.println(String.format("Teleporting players on team around %s to %.2f, %.2f, %.2f", team.getName(), spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));
            for (Player p : team.getPlayers().stream().map(Bukkit::getPlayer).filter(pl -> pl != null).collect(Collectors.toList())) {
                Location spawnAround = SpawnUtils.getSpawnAround(spawnLoc, 2);
                System.out.println(String.format("\tTeleporting %s to %.2f, %.2f, %.2f", p.getName(), spawnAround.getX(), spawnAround.getY(), spawnAround.getZ()));
                p.teleport(spawnAround);
            }
        }
        System.out.println("Spread teams!");
        System.out.println("Despawning players...");
        for (Player p : Bukkit.getOnlinePlayers()) {
            PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(p.getEntityId());
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(p.getUniqueId()))
                    continue;
                ((CraftPlayer) other).getHandle().playerConnection.sendPacket(destroyPacket);
            }
        }
        Bukkit.getServer().getScheduler().runTaskLater(UHC.plugin, () -> {
            System.out.println("Respawning players");
            for (Player p : Bukkit.getOnlinePlayers()) {
                PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(((CraftPlayer) p).getHandle());
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (p.getUniqueId().equals(other.getUniqueId()))
                        continue;
                    ((CraftPlayer) other).getHandle().playerConnection.sendPacket(spawn);
                }
            }
        }, 5L);
    }

    public void freeze() {
        int secondsPassed = Math.toIntExact((System.currentTimeMillis() - startTime) / 1000);
        System.out.println("Duration: " + duration);
        System.out.println("Seconds passed: " + secondsPassed);
        this.secondsRemaining = this.duration - secondsPassed;
        System.out.println("Seconds remaining: " + secondsRemaining);
        overworldWorldborderSize = world.getWorldBorder().getSize();
        world.getWorldBorder().setSize(overworldWorldborderSize);
        System.out.println("Worldborder size: " + overworldWorldborderSize);
        frozen_nextEndgamePhase = nextEndgamePhase;
        frozen_nextEndgamePhaseIn = nextEndgamePhaseIn;
        nextEndgamePhase = null;
        nextEndgamePhaseIn = -1;
        frozen_endgamePhase = currentEndgamePhase;
        currentEndgamePhase = NORMALGAME;
        freezeStartTime = System.currentTimeMillis();
        if (nether != null) {
            netherWorldborderSize = nether.getWorldBorder().getSize();
            nether.getWorldBorder().setSize(netherWorldborderSize);
        }
        this.state = FROZEN;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!TeamHandler.isSpectator(p)) {
                FreezeHandler.freezePlayer(p);
            }
        }
        for (Entity e : world.getEntities()) {
            if (e.getType() != EntityType.PLAYER)
                FreezeHandler.frozenEntities.put(e, e.getLocation());
        }
        world.setGameRuleValue("doDaylightCycle", "false");
        FreezeHandler.pvpEnabled = false;
        Bukkit.broadcastMessage(UtilChat.message("The game is now frozen"));
    }

    public void unfreeze() {
        if (secondsRemaining > 0) {
            world.getWorldBorder().setSize(overworldWorldborderSize);
            world.getWorldBorder().setSize(endSize, secondsRemaining);
            if (nether != null) {
                nether.getWorldBorder().setSize(netherWorldborderSize);
                nether.getWorldBorder().setSize(endSize * 2, secondsRemaining);
            }
        }
        this.state = RUNNING;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!TeamHandler.isSpectator(p)) {
                FreezeHandler.unfreeze(p);
            }
        }
        long frozenFor = System.currentTimeMillis() - freezeStartTime;
        System.out.println("Frozen for: " + frozenFor);
        this.startTime -= frozenFor;
        this.nextEndgamePhase = frozen_nextEndgamePhase;
        this.currentEndgamePhase = frozen_endgamePhase;

        if (frozen_nextEndgamePhaseIn != -1) {
            long timeRemainingOnEndgamePhase = Math.abs(freezeStartTime - frozen_nextEndgamePhaseIn);
            System.out.println("Freeze started on " + freezeStartTime);
            System.out.println("Frozen phase on " + frozen_nextEndgamePhaseIn);
            System.out.println("Time remaining on EG phase: " + timeRemainingOnEndgamePhase);
            nextEndgamePhaseIn = System.currentTimeMillis() + timeRemainingOnEndgamePhase;
        }
        Bukkit.getServer().getScheduler().runTaskLater(UHC.plugin, () -> {
            FreezeHandler.restoreBlocks();
            FreezeHandler.pvpEnabled = true;
            Bukkit.broadcastMessage(UtilChat.message("Damage enabled"));
        }, 100);
        world.setGameRuleValue("doDaylightCycle", "true");
        Bukkit.broadcastMessage(UtilChat.message("The game was frozen for " + ChatColor.GOLD + UtilTime.format(1, frozenFor, UtilTime.TimeUnit.FIT)));
        Bukkit.broadcastMessage(UtilChat.message("PvP will be enabled in 5 seconds"));
        FreezeHandler.frozenEntities.clear();
    }

    public void removePlayer(Player player) {
        Iterator<Player> players = this.players.iterator();
        while (players.hasNext()) {
            if (players.next().getUniqueId().equals(player.getUniqueId()))
                players.remove();
        }
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

    public enum EndgamePhase {
        NORMALGAME("Normal Game", -1),
        SHRINKING_WORLDBORDER("Shrinking Worldborder", 600000);
/*
        NORMALGAME("Normal Game", -1),
        HUNGER_III("Hunger III", 30000),
        POISON("Wither", 30000);
*/

        private long duration;
        private String name;

        EndgamePhase(String friendlyName, long duration) {
            this.duration = duration;
            this.name = friendlyName;
        }

        public long getDuration() {
            return duration;
        }

        public String getName() {
            return name;
        }
    }

    public static class PlayerActionBarUpdater implements Runnable {

        @Override
        public void run() {
            if (UHC.arena.state == State.RUNNING) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (TeamHandler.isSpectator(p))
                        continue;
                    TextComponent bc;
                    if (p.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
                        double distance = UHC.playerTracker.distanceToTarget(p.getUniqueId());
                        if (Double.isInfinite(distance)) {
                            bc = (TextComponent) UtilChat.generateFormattedChat("Right click to find the closest target!", net.md_5.bungee.api.ChatColor.GOLD, 8);
                        } else if (distance == PlayerTrackerHandler.DIST_IN_OTHER_DIMENSION) {
                            bc = (TextComponent) UtilChat.generateFormattedChat("The player you are tracking is in another dimension!", net.md_5.bungee.api.ChatColor.RED, 8);
                        } else {
                            bc = (TextComponent) UtilChat.generateBoldChat(UHC.playerTracker.getTarget(p.getUniqueId()).getName(), net.md_5.bungee.api.ChatColor.GOLD);
                            bc.addExtra(UtilChat.generateBoldChat(" is ", net.md_5.bungee.api.ChatColor.DARK_GREEN));
                            bc.addExtra(UtilChat.generateBoldChat(Double.toString(distance), net.md_5.bungee.api.ChatColor.GOLD));
                            bc.addExtra(UtilChat.generateBoldChat(" blocks away!", net.md_5.bungee.api.ChatColor.DARK_GREEN));
                        }
                    } else {
                        Location l = p.getLocation();
                        bc = (TextComponent) UtilChat.generateFormattedChat("Current Position: ", net.md_5.bungee.api.ChatColor.GOLD, 0);
                        bc.addExtra(UtilChat.generateFormattedChat("X: ", net.md_5.bungee.api.ChatColor.RED, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getX()), net.md_5.bungee.api.ChatColor.GREEN, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(" Y: ", net.md_5.bungee.api.ChatColor.RED, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getY()), net.md_5.bungee.api.ChatColor.GREEN, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(" Z: ", net.md_5.bungee.api.ChatColor.RED, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getZ()), net.md_5.bungee.api.ChatColor.GREEN, 0));
                    }
                    PacketPlayOutChat chat = new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + bc.toLegacyText() + "\"}"), (byte) 2);
                    ((CraftPlayer) p).getHandle().playerConnection.sendPacket(chat);
                }
            }
        }
    }
}
