package me.mrkirby153.kcuhc;

import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.command.*;
import me.mrkirby153.kcuhc.handler.*;
import me.mrkirby153.kcuhc.module.endgame.*;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.dimension.EndModule;
import me.mrkirby153.kcuhc.module.dimension.NetherModule;
import me.mrkirby153.kcuhc.module.head.DropPlayerHeadModule;
import me.mrkirby153.kcuhc.module.head.HeadAppleModule;
import me.mrkirby153.kcuhc.module.health.HardcoreHeartsModule;
import me.mrkirby153.kcuhc.module.health.NaturalRegenerationModule;
import me.mrkirby153.kcuhc.module.msc.EpisodeMarkerHandler;
import me.mrkirby153.kcuhc.module.msc.RegenTicketModule;
import me.mrkirby153.kcuhc.module.player.*;
import me.mrkirby153.kcuhc.module.tracker.CompassModule;
import me.mrkirby153.kcuhc.module.tracker.PlayerTrackerModule;
import me.mrkirby153.kcuhc.module.worldborder.BorderBumper;
import me.mrkirby153.kcuhc.module.worldborder.EndgameModule;
import me.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcuhc.module.worldborder.WorldBorderWarning;
import me.mrkirby153.kcuhc.scoreboard.ScoreboardManager;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.utils.UtilTitle;
import me.mrkirby153.kcutils.BossBar;
import me.mrkirby153.kcutils.command.CommandManager;
import me.mrkirby153.kcutils.event.UpdateEventHandler;
import me.mrkirby153.kcutils.nms.NMS;
import me.mrkirby153.kcutils.nms.NMSFactory;
import me.mrkirby153.uhc.bot.network.UHCNetwork;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class UHC extends JavaPlugin {

    public static ArrayList<String> admins;
    public static UHCNetwork uhcNetwork;
    private static UHC plugin;
    public UHCArena arena;
    public VelocityTracker velocityTracker;
    public ExtraHealthHandler extraHealthHelper;

    public TeamHandler teamHandler;
    public MOTDHandler motdHandler;
    public SpectatorHandler spectatorHandler;
    public BossBar bossBar;
    public DiscordHandler discordHandler;
    public TeamChatHandler teamChatHandler;

    private NMS nms;

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

        nms = new NMSFactory(this).getNMS();
        UtilTitle.setNms(nms);

        teamHandler = new TeamHandler(this);
        teamHandler.load();

        CommandManager.initialize(this);

        admins = (ArrayList<String>) getConfig().getStringList("admins");

        registerModules();
        ModuleRegistry.loadModulesOnStart();

        arena = new UHCArena(this, teamHandler);
        arena.initialize();

        // Load discord integration
        discordHandler = new DiscordHandler(plugin, teamHandler);
        discordHandler.load();
        uhcNetwork = discordHandler.network;

        new UpdateEventHandler(this).load();



        // Load modules
        spectatorHandler = new SpectatorHandler(this, teamHandler, nms);
        spectatorHandler.load();

        teamChatHandler = new TeamChatHandler(this);
        teamChatHandler.load();

        bossBar = new BossBar(plugin);
        bossBar.load();


        motdHandler = new MOTDHandler(this);
        motdHandler.load();

        extraHealthHelper = new ExtraHealthHandler(this);
        extraHealthHelper.load();

        velocityTracker = new VelocityTracker(this);

        getServer().getPluginManager().registerEvents(new ScoreboardManager(plugin), this);

        // Register commands
        getCommand("uhc").setExecutor(new CommandUHC(this, teamHandler));
        getCommand("team").setExecutor(new CommandTeam(teamHandler, plugin));
        getCommand("spectate").setExecutor(new CommandSpectate(teamHandler, plugin));
        getCommand("admin").setExecutor(new CommandAdmin(teamHandler));
        getCommand("teaminventory").setExecutor(new CommandTeamInv(teamHandler, plugin));
        getCommand("saycoords").setExecutor(new CommandSayCoords(teamHandler));
    }

    public void registerModules() {
        ModuleRegistry.registerModule(new NetherModule());
        ModuleRegistry.registerModule(new EndModule());
        ModuleRegistry.registerModule(new WorldBorderWarning());
        ModuleRegistry.registerModule(new CompassModule());
        ModuleRegistry.registerModule(new PlayerTrackerModule(teamHandler));
        ModuleRegistry.registerModule(new TeamInventoryModule(teamHandler));
        ModuleRegistry.registerModule(new EndgameModule());
        ModuleRegistry.registerModule(new SpreadPlayersModule());
        ModuleRegistry.registerModule(new DropPlayerHeadModule());
        ModuleRegistry.registerModule(new HeadAppleModule());
        ModuleRegistry.registerModule(new PvPGraceModule());
        ModuleRegistry.registerModule(new RegenTicketModule(teamHandler));
        ModuleRegistry.registerModule(new LoneWolfModule(teamHandler));
        ModuleRegistry.registerModule(new NaturalRegenerationModule());
        ModuleRegistry.registerModule(new BorderBumper());
        ModuleRegistry.registerModule(new WorldBorderModule());
        ModuleRegistry.registerModule(new EpisodeMarkerHandler());
        ModuleRegistry.registerModule(new PlayerPositionModule(nms, teamHandler));
        ModuleRegistry.registerModule(new HardcoreHeartsModule());
        ModuleRegistry.registerModule(new EndgameScenarioModule());

        // Register endgame scenarios
        EndgameScenarioModule.registerScenario(new TeamsEndgame());
        EndgameScenarioModule.registerScenario(new SpaceRace());
        EndgameScenarioModule.registerScenario(new FFA());

        EndgameScenarioModule.setDefault(TeamsEndgame.class);
    }

    public String serverId() {
        return getConfig().getString("discord.serverId");
    }
}
