package me.mrkirby153.kcuhc.arena;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.gui.SpectateInventory;
import me.mrkirby153.kcuhc.handler.GameListener;
import me.mrkirby153.kcuhc.handler.MOTDHandler;
import me.mrkirby153.kcuhc.handler.PregameListener;
import me.mrkirby153.kcuhc.handler.RegenTicket;
import me.mrkirby153.kcuhc.item.InventoryHandler;
import me.mrkirby153.kcuhc.noteBlock.JukeboxHandler;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_9_R1.IChatBaseComponent;
import net.minecraft.server.v1_9_R1.PacketPlayOutChat;
import net.minecraft.server.v1_9_R1.PacketPlayOutTitle;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static me.mrkirby153.kcuhc.UHC.discordHandler;
import static me.mrkirby153.kcuhc.arena.UHCArena.State.COUNTDOWN;

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
    private BossBar worldborderDist;

    private double barProgress = 1;
    private final double BAR_PROGRESS_DEC = 0.1;
    private final int WORLDBORDER_WARN_DIST = 50;

    private double worldborderInitSize;

    private final int FIREWORKS_TO_LAUNCH = 10;
    private int launchedFw = 0;

    private Color winningTeamColor = Color.WHITE;

    private List<Player> previouslyOpped = new ArrayList<>();


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
        closeToBorder = Bukkit.createBossBar(ChatColor.RED + "You are close to the worldborder!", BarColor.PINK, BarStyle.SOLID);
        countdownBar = Bukkit.createBossBar(ChatColor.RED + "Starting in ", BarColor.PINK, BarStyle.SOLID);
        worldborderDist = Bukkit.createBossBar(ChatColor.GOLD + "Worldborder " + ChatColor.RED + "X: " + ChatColor.GREEN +
                "\u00B1 ??? " + ChatColor.RED + "Z: " + ChatColor.GREEN + "\u00B1 ???", BarColor.PINK, BarStyle.SOLID);
        worldborderDist.setProgress(0);
        UHC.plugin.getServer().getPluginManager().registerEvents(this, UHC.plugin);
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
        this.state = State.ENDGAME;
    }

    public void initialize() {
        Objective health;
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
        belowName.setDisplaySlot(DisplaySlot.BELOW_NAME);
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
    }

    public void start() {
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
            PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                    IChatBaseComponent.ChatSerializer.a(String.format("{\"text\":\"%s\"}", ChatColor.GOLD + "The game has begun!")));
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(timings);
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
                InventoryHandler.instance().showHotbar(p, new SpectateInventory());
            p.setBedSpawnLocation(center, true);
            p.addPotionEffects(Arrays.asList(resist, regen, sat));
            if (p.isOp()) {
                p.setOp(false);
                previouslyOpped.add(p);
            }
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("spreadplayers %d %d %d %d true @a[team=!%s]", center.getBlockX(), center.getBlockZ(), 50, startSize / 2, TeamHandler.SPECTATORS_TEAM));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("xp -3000l @a[team=!%s]", TeamHandler.SPECTATORS_TEAM));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("achievement take * @a[team=!%s]", TeamHandler.SPECTATORS_TEAM));
        for (Entity e : world.getEntities()) {
            if (e instanceof Tameable) {
                ((Tameable) e).setOwner(null);
            }
        }
        for (Player p : players()) {
            RegenTicket.give(p);
        }
        sendEveryoneToTeamChannels();
        startingPlayers = players.size() - getSpectatorCount();
        state = State.RUNNING;
    }

    public void spectate(Player player) {
        spectate(player, false);
    }

    public void spectate(Player player, boolean switchRole) {
        TeamHandler.joinTeam(TeamHandler.spectatorsTeam(), player);
        if (switchRole) {
            if(discordHandler != null){
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
        worldborderDist.removeAll();
        HandlerList.unregisterAll(gameListener);
        UHC.plugin.getServer().getPluginManager().registerEvents(pregameListener, UHC.plugin);
        bringEveryoneToLobby();
        MOTDHandler.setMotd(ChatColor.RESET + "" + ChatColor.RED + ChatColor.MAGIC + "|..|" + ChatColor.RESET + "  " + ChatColor.GOLD + winner + ChatColor.RED + " has won the game!  " + ChatColor.RED + ChatColor.MAGIC + "|..|");
        launchedFw = 0;
        state = State.ENDGAME;
    }

    public void startCountdown() {
        JukeboxHandler.shutdown();
        countdown = 10;
        barProgress = 1;
        for (Player p : players) {
            worldborderDist.addPlayer(p);
        }
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
        worldborderDist.removeAll();
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
        dead.spigot().sendMessage(UtilChat.generateBoldChat("You have died and are now a spectator", net.md_5.bungee.api.ChatColor.RED));
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
        if (player.getWorld().getWorldBorder().getSize() == endSize) {
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
                    if (!worldborderDist.getPlayers().contains(p))
                        worldborderDist.addPlayer(p);
                }
                if (teamCountLeft() <= 1 && shouldEndCheck) {
                    if (teamsLeft().size() > 0) {
                        UHCPlayerTeam team = teamsLeft().get(0);
                        this.winningTeamColor = team.toColor();
                        stop(team.getName());
                    }
                    state = State.ENDGAME;
                }
                double[] borderDist = worldborderLoc();
                /// INIT: 300 TRAVELD: 300 - currSize
                // CurrSize: 200, therefore traveled = 100
                //
                double blocksLeft = world.getWorldBorder().getSize() - (dmStarted ? 1 : endSize);
                double percent = 1 - (blocksLeft / worldborderInitSize);
                if (percent > 1) {
                    percent = 1;
                }
                if (percent < 0)
                    percent = 0;
                worldborderDist.setProgress(percent);
                if (worldborderDist.getProgress() > 1.0)
                    worldborderDist.setProgress(1.0);
                worldborderDist.setTitle(String.format(ChatColor.GOLD + "Worldborder " + ChatColor.RED + "X: " + ChatColor.GREEN +
                        "\u00B1 %.2f " + ChatColor.RED + "Z: " + ChatColor.GREEN + "\u00B1 %.2f", borderDist[0], borderDist[1]));
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
                worldborderDist.removeAll();
                break;
        }
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
        boolean exists = false;
        for (Player p : players) {
            if (p.getUniqueId() == player.getUniqueId())
                exists = true;
        }
        if (exists) {
            return;
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
        if (TeamHandler.isSpectator(player))
            return;
        TeamHandler.leaveTeam(player);
        spectate(player);
        players.remove(player);
        Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " has been eliminated because they quit!");
    }

    public void startDeathmatch() {
        deathmatch = 30;
        for (Player p : players) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_AMBIENT, 1, 1);
            p.spigot().sendMessage(UtilChat.generateBoldChat("When deathmatch starts, the world border will shrink to 1 block over the course of 5 minutes. " +
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
        if(discordHandler == null)
            return;
        Bukkit.broadcastMessage(ChatColor.GOLD+"Creating discord channels...");
        UHC.discordHandler.createAllTeamChannels(()-> UHC.discordHandler.processAsync(()->{
            Bukkit.broadcastMessage(ChatColor.GOLD+"Assigning teams and moving everyone...");
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(UHC.plugin.serverId());
            out.writeUTF("assignTeams");
            out.writeInt((int) players.stream().filter(p -> TeamHandler.getTeamForPlayer(p) != null).count());
            players.stream().filter(p -> TeamHandler.getTeamForPlayer(p) != null).forEach(p -> {
                out.writeUTF(p.getUniqueId().toString());
                out.writeUTF(TeamHandler.getTeamForPlayer(p).getName());
            });
            UHC.discordHandler.sendMessage(out.toByteArray());
        },()->Bukkit.broadcastMessage(ChatColor.GOLD+"Everyone should now be in their discord channels")));
    }

    public void bringEveryoneToLobby() {
        if(discordHandler == null)
            return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(UHC.plugin.serverId());
        out.writeUTF("toLobby");
        UHC.discordHandler.sendMessage(out.toByteArray());
        UHC.discordHandler.deleteAllTeamChannels(null);
    }

    public enum State {
        INITIALIZED,
        GENERATING_WORLD,
        WAITING,
        COUNTDOWN,
        RUNNING,
        ENDGAME
    }

    public static class PlayerActionBarUpdater implements Runnable {

        @Override
        public void run() {
            if (UHC.arena.state == State.RUNNING) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location l = p.getLocation();
                    TextComponent bc = (TextComponent) UtilChat.generateFormattedChat("Current Position: ", net.md_5.bungee.api.ChatColor.GOLD, 0);
                    bc.addExtra(UtilChat.generateFormattedChat("X: ", net.md_5.bungee.api.ChatColor.RED, 0));
                    bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getX()), net.md_5.bungee.api.ChatColor.GREEN, 0));
                    bc.addExtra(UtilChat.generateFormattedChat(" Y: ", net.md_5.bungee.api.ChatColor.RED, 0));
                    bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getY()), net.md_5.bungee.api.ChatColor.GREEN, 0));
                    bc.addExtra(UtilChat.generateFormattedChat(" Z: ", net.md_5.bungee.api.ChatColor.RED, 0));
                    bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getZ()), net.md_5.bungee.api.ChatColor.GREEN, 0));
                    PacketPlayOutChat chat = new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + bc.toLegacyText() + "\"}"), (byte) 2);
                    ((CraftPlayer) p).getHandle().playerConnection.sendPacket(chat);
                }
            }
        }
    }
}
