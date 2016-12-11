package me.mrkirby153.kcuhc;

import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.command.*;
import me.mrkirby153.kcuhc.handler.*;
import me.mrkirby153.kcuhc.handler.listener.SpectateListener;
import me.mrkirby153.kcuhc.scoreboard.ScoreboardManager;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcutils.BossBar;
import me.mrkirby153.uhc.bot.network.UHCNetwork;
import me.mrkirby153.uhc.bot.network.data.RedisConnection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

public class UHC extends JavaPlugin {

    public static ArrayList<String> admins;
    public static UHCNetwork uhcNetwork;
    private static UHC plugin;
    public UHCArena arena;
    public SpectateListener spectateListener;
    public PlayerTrackerHandler playerTracker;
    public VelocityTracker velocityTracker;
    public ExtraHealthHandler extraHealthHelper;

    public TeamHandler teamHandler;
    public MOTDHandler motdHandler;
    public BorderBumper borderBumper;
    public EpisodeMarkerHandler markerHandler;
    public SpectatorHandler spectatorHandler;
    public BossBar bossBar;

    public static UHC getInstance() {
        return plugin;
    }

    public static boolean isAdmin(Player player) {
        return admins.contains(player.getName());
    }

    public static boolean isAdmin(String string) {
        return admins.contains(string);
    }

    @Override
    public void onDisable() {
        teamHandler.unregisterAll();
        // Remove everyone's boss bar in case of a reload
        bossBar.removeAll();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        plugin = this;

        teamHandler = new TeamHandler(this);
        teamHandler.load();

        admins = (ArrayList<String>) getConfig().getStringList("admins");

        if (new File(getDataFolder(), "arena.yml").exists()) {
            arena = new UHCArena(this, teamHandler);
            arena.initialize();
        }

        if (getConfig().getBoolean("discord.useDiscord")) {
            getCommand("discord").setExecutor(new CommandDiscord(teamHandler, plugin));
            String botHost = getConfig().getString("discord.botHost");
            int botPort = getConfig().getInt("discord.botPort");
            String password = getConfig().getString("discord.botPassword");
            uhcNetwork = new UHCNetwork(new RedisConnection(botHost, botPort, password.equals("") ? null : password));
            if (getConfig().getString("discord.serverId") == null || getConfig().getString("discord.serverId").isEmpty()) {
                String acceptableChars = "ABCEDFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                Random r = new SecureRandom();
                String id = "";
                for (int i = 0; i < 15; i++) {
                    id += acceptableChars.charAt(r.nextInt(acceptableChars.length()));
                }
                getConfig().set("discord.serverId", id);
                saveConfig();
                getLogger().info("Set server id to " + id);
            }
        }

        new FreezeHandler(this);


        // Load modules
        spectatorHandler = new SpectatorHandler(this, teamHandler);
        spectatorHandler.load();

        bossBar = new BossBar(plugin);
        bossBar.load();

        playerTracker = new PlayerTrackerHandler(this, teamHandler);
        playerTracker.load();

        motdHandler = new MOTDHandler(this);
        motdHandler.load();

        borderBumper = new BorderBumper(this);
        borderBumper.load();

        markerHandler = new EpisodeMarkerHandler(this);
        markerHandler.load();

        extraHealthHelper = new ExtraHealthHandler(this);
        extraHealthHelper.load();

        velocityTracker = new VelocityTracker(this);


        getServer().getPluginManager().registerEvents(new ScoreboardManager(plugin), this);
        getServer().getPluginManager().registerEvents(new RegenTicket(), this);

        // Register commands
        getCommand("uhc").setExecutor(new CommandUHC(this, teamHandler));
        getCommand("team").setExecutor(new CommandTeam(teamHandler, plugin));
        getCommand("spectate").setExecutor(new CommandSpectate(teamHandler, plugin));
        getCommand("admin").setExecutor(new CommandAdmin(teamHandler));
        getCommand("teaminventory").setExecutor(new CommandTeamInv(teamHandler, plugin));

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new UHCArena.PlayerActionBarUpdater(teamHandler), 0, 1);
    }

    public String serverId() {
        return getConfig().getString("discord.serverId");
    }
}
