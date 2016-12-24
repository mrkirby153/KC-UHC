package me.mrkirby153.kcuhc;

import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.arena.handler.lonewolf.LoneWolfHandler;
import me.mrkirby153.kcuhc.command.*;
import me.mrkirby153.kcuhc.handler.*;
import me.mrkirby153.kcuhc.scoreboard.ScoreboardManager;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcutils.BossBar;
import me.mrkirby153.kcutils.command.CommandManager;
import me.mrkirby153.uhc.bot.network.UHCNetwork;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class UHC extends JavaPlugin {

    public static ArrayList<String> admins;
    public static UHCNetwork uhcNetwork;
    private static UHC plugin;
    public UHCArena arena;
    public PlayerTrackerHandler playerTracker;
    public VelocityTracker velocityTracker;
    public ExtraHealthHandler extraHealthHelper;

    public TeamHandler teamHandler;
    public MOTDHandler motdHandler;
    public BorderBumper borderBumper;
    public EpisodeMarkerHandler markerHandler;
    public SpectatorHandler spectatorHandler;
    public BossBar bossBar;
    public DiscordHandler discordHandler;
    public TeamChatHandler teamChatHandler;
    public LoneWolfHandler loneWolfHandler;

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

        CommandManager.initialize(this);

        admins = (ArrayList<String>) getConfig().getStringList("admins");

        arena = new UHCArena(this, teamHandler);
        arena.initialize();

        // Load discord integration
        discordHandler = new DiscordHandler(plugin, teamHandler);
        discordHandler.load();
        uhcNetwork = discordHandler.network;

        new FreezeHandler(this);


        // Load modules
        spectatorHandler = new SpectatorHandler(this, teamHandler);
        spectatorHandler.load();

        teamChatHandler = new TeamChatHandler(this);
        teamChatHandler.load();

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

        loneWolfHandler = new LoneWolfHandler(this, teamHandler);
        loneWolfHandler.load();


        getServer().getPluginManager().registerEvents(new ScoreboardManager(plugin), this);
        getServer().getPluginManager().registerEvents(new RegenTicket(), this);

        // Register commands
        getCommand("uhc").setExecutor(new CommandUHC(this, teamHandler));
        getCommand("team").setExecutor(new CommandTeam(teamHandler, plugin));
        getCommand("spectate").setExecutor(new CommandSpectate(teamHandler, plugin));
        getCommand("admin").setExecutor(new CommandAdmin(teamHandler));
        getCommand("teaminventory").setExecutor(new CommandTeamInv(teamHandler, plugin));
        getCommand("saycoords").setExecutor(new CommandSayCoords(teamHandler));

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new UHCArena.PlayerActionBarUpdater(teamHandler), 0, 1);
    }

    public String serverId() {
        return getConfig().getString("discord.serverId");
    }
}
