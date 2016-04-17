package me.mrkirby153.kcuhc;

import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.TeamSpectator;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.command.CommandMusic;
import me.mrkirby153.kcuhc.command.CommandSpectate;
import me.mrkirby153.kcuhc.command.CommandTeam;
import me.mrkirby153.kcuhc.command.CommandUHC;
import me.mrkirby153.kcuhc.discord.DiscordBotConnection;
import me.mrkirby153.kcuhc.discord.DiscordHandler;
import me.mrkirby153.kcuhc.handler.MOTDHandler;
import me.mrkirby153.kcuhc.handler.RegenTicket;
import me.mrkirby153.kcuhc.handler.SpectateListener;
import me.mrkirby153.kcuhc.item.InventoryHandler;
import me.mrkirby153.kcuhc.noteBlock.JukeboxHandler;
import net.dv8tion.jda.JDA;
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
    public static JDA jda;

    @Deprecated
    public static DiscordHandler handler;

    public static SpectateListener spectateListener;


    public static DiscordBotConnection discordHandler;

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
            }
            getCommand("discord").setExecutor(new CommandDiscord());*/
            discordHandler = new DiscordBotConnection(getConfig().getString("discord.botHost"), getConfig().getInt("discord.botPort"));
            discordHandler.connect();
            if (getConfig().getString("discord.serverId") == null || getConfig().getString("discord.serverId").isEmpty()) {
                String acceptableChars = "ABCEDFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                Random r = new SecureRandom();
                String id = "";
                for (int i = 0; i < 15; i++) {
                    id += acceptableChars.charAt(r.nextInt(acceptableChars.length()));
                }
                getConfig().set("discsord.serverId", id);
                saveConfig();
                getLogger().info("Set server id to " + id);
            }
        }
        getServer().getPluginManager().registerEvents(new MOTDHandler(), this);
        getServer().getPluginManager().registerEvents(spectateListener = new SpectateListener(), this);
        getServer().getPluginManager().registerEvents(InventoryHandler.instance(), this);
        getServer().getPluginManager().registerEvents(new JukeboxHandler(), this);
        getServer().getPluginManager().registerEvents(new RegenTicket(), this);
        TeamHandler.registerTeam(TeamHandler.SPECTATORS_TEAM, new TeamSpectator());
        TeamHandler.loadFromFile();
        getCommand("uhc").setExecutor(new CommandUHC());
        getCommand("team").setExecutor(new CommandTeam());
        getCommand("spectate").setExecutor(new CommandSpectate());
        getCommand("music").setExecutor(new CommandMusic());
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new TeamSpectator.SpectateTask(), 0, 2);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new UHCArena.PlayerActionBarUpdater(), 0, 1);
    }


    @Override
    public void onDisable() {
        TeamHandler.unregisterAll();
        JukeboxHandler.shutdown();
        if (handler != null)
            handler.shutdown();
        if (jda != null)
            jda.shutdown();
    }

    public static boolean isAdmin(Player player) {
        return admins.contains(player.getName());
    }

    public static boolean isAdmin(String string) {
        return admins.contains(string);
    }
}
