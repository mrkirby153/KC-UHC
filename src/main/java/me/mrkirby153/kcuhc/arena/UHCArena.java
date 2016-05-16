package me.mrkirby153.kcuhc.arena;


import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.UtilTime;
import me.mrkirby153.kcuhc.gui.SpecInventory;
import me.mrkirby153.kcuhc.handler.GameListener;
import me.mrkirby153.kcuhc.handler.MOTDHandler;
import me.mrkirby153.kcuhc.handler.PregameListener;
import me.mrkirby153.kcuhc.handler.RegenTicket;
import me.mrkirby153.kcuhc.noteBlock.JukeboxHandler;
import me.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcuhc.shop.item.NullEnchantment;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_9_R2.IChatBaseComponent;
import net.minecraft.server.v1_9_R2.PacketPlayOutChat;
import net.minecraft.server.v1_9_R2.PacketPlayOutTitle;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
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
    private HashMap<UUID, String> originalNames = new HashMap<>();

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

    private BossBar closeToBorder;
    private BossBar countdownBar;

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


    public UHCArena(World world, int startSize, int endSize, int duration, Location center) {
        this.world = world;
        this.nether = Bukkit.getWorld(world.getName() + "_nether");
        this.startSize = startSize;
        this.endSize = endSize;
        this.duration = duration;
        this.center = world.getHighestBlockAt(center.getBlockX(), center.getBlockZ()).getLocation();
        minX = center.getBlockX() - startSize;
        maxX = center.getBlockX() + startSize;
        minZ = center.getBlockZ() - startSize;
        maxZ = center.getBlockZ() + startSize;
        UHC.plugin.getServer().getPluginManager().registerEvents(pregameListener, UHC.plugin);
        UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, this, 0, 20);
        UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, this::drawScoreboard, 0, 1L);
//        UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, this::updateEndgame, 0L, 2L);
        UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, this::endgameEffect, 0L, 1L);
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
        closeToBorder = Bukkit.createBossBar(ChatColor.RED + "You are close to the worldborder!", BarColor.PINK, BarStyle.SOLID);
        countdownBar = Bukkit.createBossBar(ChatColor.RED + "Starting in ", BarColor.PINK, BarStyle.SOLID);
        UHC.plugin.getServer().getPluginManager().registerEvents(this, UHC.plugin);
        this.scoreboard = new UHCScoreboard();
        craftingRecipies();
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
        Bukkit.broadcastMessage(ChatColor.GOLD + "World pregeneration complete!");
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
/*        Objective health;
        if (board.getObjective("health") == null)
            health = board.registerNewObjective("health", "health");
        else
            health = board.getObjective("health");
        health.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        Objective belowName;
        if (board.getObjective("belowName") != null) {
            board.getObjective("belowName").unregister();
        }
        belowName = board.registerNewObjective("belowName", "health");
        char heart = '\u2764';
        belowName.setDisplayName(ChatColor.RED + Character.toString(heart));
        belowName.setDisplaySlot(DisplaySlot.BELOW_NAME);*/
        currentEndgamePhase = EndgamePhase.NORMALGAME;
        nextEndgamePhase = null;
        nextEndgamePhaseIn = -1;
        scoreboard.createTeams();
        WorldBorder border = world.getWorldBorder();
        border.setSize(60);
        border.setWarningDistance(0);
        if (nether != null && nether.getWorldBorder() != null) {
            nether.getWorldBorder().setSize(60);
            nether.getWorldBorder().setWarningDistance(0);
        }
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doMobLoot", "false");
        world.setTime(1200);
        Bukkit.broadcastMessage(ChatColor.GOLD + "Initialized and ready!");
        MOTDHandler.setMotd(ChatColor.GREEN + "Ready");
        players = new ArrayList<>();
        JukeboxHandler.startJukebox();
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
        currentEndgamePhase = EndgamePhase.NORMALGAME;
        nextEndgamePhase = null;
        nextEndgamePhaseIn = -1;
        WorldBorder border = world.getWorldBorder();
        border.setSize(startSize);
        border.setWarningDistance(WORLDBORDER_WARN_DIST);
        if (nether != null && nether.getWorldBorder() != null) {
            nether.getWorldBorder().setSize(startSize * 2);
            nether.getWorldBorder().setWarningDistance(WORLDBORDER_WARN_DIST);
        }
        setWorldborder(endSize, duration);
        HandlerList.unregisterAll(pregameListener);
        UHC.plugin.getServer().getPluginManager().registerEvents(gameListener, UHC.plugin);
        world.setGameRuleValue("naturalRegeneration", "false");
        world.setGameRuleValue("doMobSpawning", "true");
        world.setGameRuleValue("doMobLoot", "true");
        world.setGameRuleValue("doDaylightCycle", "true");
        countdownBar.removeAll();
        PotionEffect resist = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 6, true, false);
        PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 30 * 20, 10, true, false);
        PotionEffect sat = new PotionEffect(PotionEffectType.SATURATION, 30 * 20, 20, true, false);
        for (Player p : players) {
            PacketPlayOutTitle timings = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, 10, 20 * 5, 20);
            PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, IChatBaseComponent.ChatSerializer.a("{\"text\":\"\"}"));
            PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                    IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.GOLD + "The game has begun!")));
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
        }
/*        // TODO: 5/13/2016 Write spreadplayers algorithm, as args no longer work :(
        String format = String.format("spreadplayers %d %d %d %d true @a[team=!%s]", center.getBlockX(), center.getBlockZ(), 50, startSize / 2, TeamHandler.SPECTATORS_TEAM);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), format);*/
        if (shouldSpreadPlayers)
            new SpreadPlayersHandler().execute(world.getName(), center.getBlockX(), center.getBlockZ(), 50, startSize / 2);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "xp -3000l @a");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "achievement take * @a");
        for (Entity e : world.getEntities()) {
            if (e instanceof Tameable) {
                ((Tameable) e).setOwner(null);
            }
            if (e.getType() == EntityType.DROPPED_ITEM) {
                e.remove();
            }
        }
        for (Player p : players()) {
            RegenTicket.give(p);
        }
        sendEveryoneToTeamChannels();
        startingPlayers = players.size() - getSpectatorCount();
        state = State.RUNNING;
        startTime = System.currentTimeMillis();
    }

    public void spectate(Player player) {
        spectate(player, false);
    }

    public void spectate(Player player, boolean switchRole) {
        TeamHandler.joinTeam(TeamHandler.spectatorsTeam(), player);
        if (switchRole) {
            if (discordHandler != null) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(UHC.plugin.serverId());
                out.writeUTF("assignRole");
                out.writeUTF(player.getUniqueId().toString());
                out.writeUTF(TeamHandler.spectatorsTeam().getName());
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
        JukeboxHandler.nextSong();
        WorldBorder border = world.getWorldBorder();
        border.setSize(60);
        border.setWarningDistance(0);
        if (nether != null) {
            nether.getWorldBorder().setSize(60);
            nether.getWorldBorder().setWarningDistance(0);
        }
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
        state = ENDGAME;
    }

    public void startCountdown() {
        JukeboxHandler.shutdown();
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
        JukeboxHandler.shutdown();
        HandlerList.unregisterAll(UHC.plugin);
        Bukkit.getServer().getScheduler().cancelTasks(UHC.plugin);
        setWorldborder(world.getWorldBorder().getSize() + 150, 0);
        countdownBar.removeAll();
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
        closeToBorder.removePlayer(dead);
        players.remove(dead);
        dead.spigot().sendMessage(generateBoldChat("You have died and are now a spectator", net.md_5.bungee.api.ChatColor.RED));
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

    private void warnAboutWorldBorder(Player player) {
        if (dmStarted)
            return;
        if (player.getWorld().getWorldBorder().getSize() == endSize || currentEndgamePhase == SHRINKING_WORLDBORDER) {
            if (closeToBorder.getPlayers().contains(player))
                closeToBorder.removePlayer(player);
            return;
        }
        WorldBorder wb = player.getWorld().getWorldBorder();
        Location wb_c = wb.getCenter();
        double wb_x = Math.abs(wb_c.getX() + (wb.getSize() / 2)) - 1;
        double wb_z = Math.abs(wb_c.getZ() + (wb.getSize() / 2)) - 1;
        double pLocX = Math.abs(player.getLocation().getX());
        double pLocZ = Math.abs(player.getLocation().getZ());
        double distX = wb_x - pLocX;
        double distZ = wb_z - pLocZ;
        double targetX = wb_x - WORLDBORDER_WARN_DIST;
        double targetZ = wb_z - WORLDBORDER_WARN_DIST;
        if (distX < distZ) {
            if (pLocX > targetX) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, scaleSound(distX), 0.5f);
                if (!closeToBorder.getPlayers().contains(player))
                    closeToBorder.addPlayer(player);
            } else {
                if (closeToBorder.getPlayers().contains(player))
                    closeToBorder.removePlayer(player);
            }
        } else if (pLocZ > targetZ) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, scaleSound(distZ), 2);
            if (!closeToBorder.getPlayers().contains(player))
                closeToBorder.addPlayer(player);
        } else {
            if (closeToBorder.getPlayers().contains(player))
                closeToBorder.removePlayer(player);
        }
    }

    private float scaleSound(double dist) {
        if (dist > WORLDBORDER_WARN_DIST)
            dist = WORLDBORDER_WARN_DIST;
        if (dist < 0)
            dist = 0;
        return (float) (2.0 - (2.0f / WORLDBORDER_WARN_DIST) * dist);
    }

    public static UHCArena loadFromFile() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(UHC.plugin.getDataFolder(), "arena.yml"));
        UHCArena uhcArena = new UHCArena(Bukkit.getWorld(cfg.getString("world")), cfg.getInt("startSize"), cfg.getInt("endSize"), cfg.getInt("duration"), (Location) cfg.get("startingPoint"));
        uhcArena.setPercentToGlow(cfg.getInt("glowPercent"));
        return uhcArena;
    }

    @Override
    public void run() {
        switch (state) {
            case INITIALIZED:
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setAllowFlight(true);
                }
                break;
            case COUNTDOWN:
                if (countdown > 0) {
                    for (Player p : players) {
                        if (!countdownBar.getPlayers().contains(p)) {
                            countdownBar.addPlayer(p);
                        }
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 1);
                        PacketPlayOutTitle timings = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, 0, 21, 0);
                        PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE,
                                IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.GREEN + "Starting in")));
                        PacketPlayOutTitle subtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                                IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.RED + "" + countdown)));
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(timings);
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(title);
                        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(subtitle);
                    }
                    MOTDHandler.setMotd(ChatColor.YELLOW + "Starting in " + countdown);
                    if (countdown > 5)
                        countdownBar.setColor(BarColor.GREEN);
                    else if (countdown <= 5 && countdown > 3)
                        countdownBar.setColor(BarColor.YELLOW);
                    else
                        countdownBar.setColor(BarColor.RED);
                    countdownBar.setProgress(barProgress);
                    countdownBar.setTitle((((countdown % 2) == 0) ? ChatColor.BOLD + "" + ChatColor.RED : ChatColor.BOLD + "" + ChatColor.GREEN) + "Starting in " + countdown + " seconds");
                    countdown--;
                    barProgress -= BAR_PROGRESS_DEC;
                } else {
                    start();
                }
                break;
            case RUNNING:
                MOTDHandler.setMotd(ChatColor.RED + "Game in progress. " + ChatColor.AQUA + "" + (players.size() - getSpectatorCount()) + ChatColor.RED + " alive");
                if (closeToBorder.getTitle().equalsIgnoreCase(ChatColor.RED + "!!! You are close to the worldborder !!!")) {
                    closeToBorder.setTitle(ChatColor.RED + "" + ChatColor.BOLD + "!! You are close to the worldborder !!");
                } else {
                    closeToBorder.setTitle(ChatColor.RED + "!!! You are close to the worldborder !!!");
                }
                for (Player p : players) {
                    p.setGlowing(false);
                    if (!TeamHandler.isSpectator(p))
                        warnAboutWorldBorder(p);
                }
                if (teamCountLeft() <= 1 && shouldEndCheck) {
                    if (teamsLeft().size() > 0) {
                        UHCPlayerTeam team = teamsLeft().get(0);
                        this.winningTeamColor = team.toColor();
                        stop(team.getName());
                    }
                    state = ENDGAME;
                }
                if (world.getWorldBorder().getSize() == endSize) {
                    world.getWorldBorder().setWarningDistance(0);
                }
                if (nether != null)
                    if (nether.getWorldBorder().getSize() == endSize)
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
                if (this.launchedFw++ < this.FIREWORKS_TO_LAUNCH) {
                    int distFromWB = 16;
                    double worldborderRadius = world.getWorldBorder().getSize() / 2d;
                    Location pZX = center.clone().add(worldborderRadius - distFromWB, 6, worldborderRadius - distFromWB);
                    Location pXnZ = center.clone().add(worldborderRadius - distFromWB, 6, -(worldborderRadius - distFromWB));
                    Location pZnX = center.clone().add(-(worldborderRadius - distFromWB), 6, worldborderRadius - distFromWB);
                    Location nXZ = center.clone().add(-(worldborderRadius - distFromWB), 6, -(worldborderRadius - distFromWB));

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
                }
                for (Player p : Bukkit.getOnlinePlayers())
                    p.setGlowing(false);
                break;
        }
        updateDisconnect();
        drawScoreboard();
    }

    private int runCount = 0;

    private void updateEndgame() {
        if (state != RUNNING)
            return;
        switch (currentEndgamePhase) {
            case NORMALGAME:
                if (world.getWorldBorder().getSize() <= endSize) {
                    if (nextEndgamePhase != EndgamePhase.HUNGER_III) {
                        nextEndgamePhaseIn = System.currentTimeMillis() + EndgamePhase.HUNGER_III.getDuration();
                        nextEndgamePhase = EndgamePhase.HUNGER_III;
                        firstAnnounce = true;
                    }
                }
                break;
            case HUNGER_III:
                if (nextEndgamePhase != EndgamePhase.SHRINKING_WORLDBORDER) {
                    nextEndgamePhaseIn = System.currentTimeMillis() + EndgamePhase.SHRINKING_WORLDBORDER.getDuration();
                    nextEndgamePhase = EndgamePhase.SHRINKING_WORLDBORDER;
                    firstAnnounce = true;
                }
                break;
            case SHRINKING_WORLDBORDER:
                runCount++;
                if (runCount % 40 == 0) {
                    runCount = 0;
                    WorldBorder wb = world.getWorldBorder();
                    if (wb.getSize() > 1)
                        wb.setSize(wb.getSize() - 1, 1);
                }
                nextEndgamePhase = null;
                break;
        }
        if (nextEndgamePhase != null && currentEndgamePhase != SHRINKING_WORLDBORDER)
            announcePhase(nextEndgamePhase);
        if (currentEndgamePhase != SHRINKING_WORLDBORDER)
            activatePhase();
    }

    private void endgameEffect() {
        if (state != RUNNING)
            return;
        switch (currentEndgamePhase) {
            case SHRINKING_WORLDBORDER:
            case HUNGER_III:
                for (Player p : players()) {
                    if (TeamHandler.isSpectator(p))
                        continue;
                    if (!p.hasPotionEffect(PotionEffectType.HUNGER)) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 2, false, true));
                    }
                }
                break;
        }
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
                            scoreboard.add(onlineCount.get(t) + " " + TeamHandler.getTeamByName(t).getColor() + WordUtils.capitalizeFully(t.replace('_', ' ')));
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void entityDamageEvent(EntityDamageEvent event) {
        if (event.isCancelled())
            return;
        if (event.getEntity().getType().equals(EntityType.PLAYER))
            return;
        if (!event.getEntity().getType().isAlive())
            return;
        updateEntityDamageName((LivingEntity) event.getEntity(), ((LivingEntity) event.getEntity()).getHealth() - event.getFinalDamage());
    }

    private void craftingRecipies() {
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
                    apple.addEnchantment(new NullEnchantment(), 1);
//                    apple.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
                    inv.setResult(apple);
                    return;
                }
            }
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
            player.sendRawMessage(ChatColor.GREEN + "To Craft: " + ChatColor.WHITE + "Use the recipie for a Golden Apple, but replace the apple with the head");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ChatColor.BOLD + "=============================================");
        }
    }

    @EventHandler
    public void consumeHeadApple(PlayerItemConsumeEvent event) {
        if (event.getItem().getItemMeta().getDisplayName() == null)
            return;
        if (!event.getItem().getItemMeta().getDisplayName().contains("Head"))
            return;
        event.getPlayer().sendMessage(ChatColor.BLUE + "> " + ChatColor.WHITE + "You ate a head apple!");
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 1));
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void entityHeal(EntityRegainHealthEvent event) {
        if (event.isCancelled())
            return;
        if (event.getEntity().getType().equals(EntityType.PLAYER))
            return;
        if (!event.getEntity().getType().isAlive())
            return;
        updateEntityDamageName((LivingEntity) event.getEntity(), ((LivingEntity) event.getEntity()).getHealth() + event.getAmount());
    }


    private void updateEntityDamageName(LivingEntity le, double damage) {
        String customName = le.getCustomName() != null ? le.getCustomName() : le.getName();
        if (!originalNames.containsKey(le.getUniqueId())) {
            originalNames.put(le.getUniqueId(), customName);
        }
        customName = originalNames.get(le.getUniqueId());
        if (damage >= le.getMaxHealth())
            damage = le.getMaxHealth();
        if (damage < 0)
            damage = 0;
        le.setCustomName("[" + ChatColor.RED + (int) damage + "/" + (int) le.getMaxHealth() + ChatColor.RESET + "] " + customName);
        le.setCustomNameVisible(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void entityDeathEvent(EntityDeathEvent event) {
        originalNames.remove(event.getEntity().getUniqueId());
    }


    private boolean entityExists(UUID u, World world) {
        for (Entity e : world.getEntities()) {
            if (e.getUniqueId().equals(u)) {
                return true;
            }
        }
        return false;
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
        setWorldborder(1, 60 * 5);
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
            Bukkit.broadcastMessage("Checking if we should end");
        else
            Bukkit.broadcastMessage("No longer checking if we should end");
    }

    public void toggleSpreadingPlayers() {
        this.shouldSpreadPlayers = !this.shouldSpreadPlayers;
        if (shouldSpreadPlayers) {
            Bukkit.broadcastMessage("Spreading players when the game starts");
        } else {
            Bukkit.broadcastMessage("No longer spreading players");
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
        Bukkit.broadcastMessage(generateBoldChat(player.getName() + " has disconnected! They have 5 minutes to log back in before they are" +
                " eliminated!", net.md_5.bungee.api.ChatColor.WHITE).toLegacyText());
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
                uuidToStringMap.remove(entry.getKey());
            }
        }
    }

    public void playerJoin(Player player) {
        this.logoutTimes.remove(player.getUniqueId());
        if (queuedTeamRemovals.contains(player.getUniqueId())) {
            queuedTeamRemovals.remove(player.getUniqueId());
            TeamHandler.leaveTeam(player);
            spectate(player);
            players.remove(player);
            player.spigot().sendMessage(UtilChat.generateFormattedChat("Due to you being logged off for more than 5 minutes, you have been removed from the game",
                    net.md_5.bungee.api.ChatColor.GOLD, 8));
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

    public void setWorldborder(double radius, int time) {
        worldborderInitSize = world.getWorldBorder().getSize();
        System.out.println("Worldborder init Size: " + worldborderInitSize);
        world.getWorldBorder().setSize(radius, time);
        if (nether != null)
            nether.getWorldBorder().setSize(radius * 2, time);
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
        Bukkit.broadcastMessage(ChatColor.GOLD + "Creating discord channels...");
        UHC.discordHandler.createAllTeamChannels(() -> UHC.discordHandler.processAsync(() -> {
            Bukkit.broadcastMessage(ChatColor.GOLD + "Assigning teams and moving everyone...");
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(UHC.plugin.serverId());
            out.writeUTF("assignTeams");
            out.writeInt((int) players.stream().filter(p -> TeamHandler.getTeamForPlayer(p) != null).count());
            players.stream().filter(p -> TeamHandler.getTeamForPlayer(p) != null).forEach(p -> {
                out.writeUTF(p.getUniqueId().toString());
                out.writeUTF(TeamHandler.getTeamForPlayer(p).getName());
            });
            UHC.discordHandler.sendMessage(out.toByteArray());
        }, () -> Bukkit.broadcastMessage(ChatColor.GOLD + "Everyone should now be in their discord channels")));
    }

    public void bringEveryoneToLobby() {
        if (discordHandler == null)
            return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(UHC.plugin.serverId());
        out.writeUTF("toLobby");
        UHC.discordHandler.sendMessage(out.toByteArray());
        UHC.discordHandler.deleteAllTeamChannels(null);
    }

    public void setEndgamePhase(EndgamePhase endgamePhase) {
        this.currentEndgamePhase = endgamePhase;
    }

    public enum State {
        INITIALIZED,
        GENERATING_WORLD,
        WAITING,
        COUNTDOWN,
        RUNNING,
        ENDGAME
    }

    public enum EndgamePhase {
        NORMALGAME("Normal Game", -1),
        HUNGER_III("Hunger III", 300000),
        SHRINKING_WORLDBORDER("Shrinking Worldborder", 900000);
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
