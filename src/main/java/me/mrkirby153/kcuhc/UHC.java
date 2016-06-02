package me.mrkirby153.kcuhc;

import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.TeamSpectator;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.command.*;
import me.mrkirby153.kcuhc.discord.DiscordBotConnection;
import me.mrkirby153.kcuhc.handler.*;
import me.mrkirby153.kcuhc.noteBlock.JukeboxHandler;
import me.mrkirby153.kcuhc.scoreboard.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

public class UHC extends JavaPlugin {

    public static UHC plugin;

    public static UHCArena arena;

    public static ArrayList<String> admins;

    public static SpectateListener spectateListener;


    public static DiscordBotConnection discordHandler;

    public static PlayerTrackerHandler playerTracker;

    public static VelocityTracker velocityTracker;

    public static EpisodeMarkerHandler markerHandler;

    public static ExtraHealthHandler extraHealthHelper;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        plugin = this;
        JukeboxHandler.initJukebox(new File(getDataFolder(), "songs"));
        admins = (ArrayList<String>) getConfig().getStringList("admins");
        if (new File(getDataFolder(), "arena.yml").exists()) {
            arena = UHCArena.loadFromFile();
            arena.initialize();
        }
        if (getConfig().getBoolean("discord.useDiscord")) {
/*            try {
                String botGuild = getConfig().getString("discord.botGuild");
                handler = new DiscordHandler(botGuild);
                String botToken = getConfig().getString("discord.botToken");
                getLogger().info("Connecting to discord using the token '" + botToken + "' and the guild '" + botGuild + "'");
                new JDABuilder(botToken).addListener(handler).buildBlocking();
            } catch (LoginException | InterruptedException e) {
                e.printStackTrace();
            }*/
            getCommand("discord").setExecutor(new CommandDiscord());
            discordHandler = new DiscordBotConnection(getConfig().getString("discord.botHost"), getConfig().getInt("discord.botPort"));
            discordHandler.connect();
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
        new SpectatorTask(this);
        new FreezeHandler(this);
        playerTracker = new PlayerTrackerHandler(this);
        velocityTracker = new VelocityTracker(this);
        markerHandler = new EpisodeMarkerHandler(this);
        extraHealthHelper = new ExtraHealthHandler(this);
        getServer().getPluginManager().registerEvents(new MOTDHandler(), this);
        getServer().getPluginManager().registerEvents(spectateListener = new SpectateListener(), this);
        getServer().getPluginManager().registerEvents(new JukeboxHandler(), this);
        getServer().getPluginManager().registerEvents(new ScoreboardManager(), this);
        getServer().getPluginManager().registerEvents(new RegenTicket(), this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new BorderBumper(),0, 1);
        TeamHandler.registerTeam(TeamHandler.SPECTATORS_TEAM, new TeamSpectator());
        TeamHandler.loadFromFile();
        getCommand("uhc").setExecutor(new CommandUHC());
        getCommand("team").setExecutor(new CommandTeam());
        getCommand("spectate").setExecutor(new CommandSpectate());
        getCommand("music").setExecutor(new CommandMusic());
        getCommand("admin").setExecutor(new CommandAdmin());
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new UHCArena.PlayerActionBarUpdater(), 0, 1);
    }


    @Override
    public void onDisable() {
        TeamHandler.unregisterAll();
        JukeboxHandler.shutdown();
        if (discordHandler != null)
            discordHandler.shutdown();
    }

    public static boolean isAdmin(Player player) {
        return admins.contains(player.getName());
    }

    public static boolean isAdmin(String string) {
        return admins.contains(string);
    }

    public String serverId() {
        return getConfig().getString("discord.serverId");
    }
}
