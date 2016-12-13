package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.command.CommandDiscord;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcutils.Module;
import me.mrkirby153.uhc.bot.network.UHCNetwork;
import me.mrkirby153.uhc.bot.network.data.RedisConnection;

import java.security.SecureRandom;
import java.util.Random;

public class DiscordHandler extends Module<UHC> {

    public UHCNetwork network;
    private TeamHandler teamHandler;

    public DiscordHandler(UHC plugin, TeamHandler teamHandler) {
        super("Discord", "1.0", plugin);
        this.teamHandler = teamHandler;
    }

    @Override
    protected void init() {
        if (getConfig().getBoolean("discord.useDiscord")) {
            getPlugin().getCommand("discord").setExecutor(new CommandDiscord(teamHandler, getPlugin()));

            String botHost = getConfig().getString("discord.botHost");
            int botPort = getConfig().getInt("discord.botPort");
            String password = getConfig().getString("discord.botPassword");
            network = new UHCNetwork(new RedisConnection(botHost, botPort, password.equals("") ? null : password));

            // Generate the server id if it does not exist
            if (getConfig().getString("discord.serverId") == null || getConfig().getString("discord.serverId").isEmpty()) {
                String acceptableChars = "ABCEDFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                Random r = new SecureRandom();
                String id = "";
                for (int i = 0; i < 15; i++) {
                    id += acceptableChars.charAt(r.nextInt(acceptableChars.length()));
                }
                getConfig().set("discord.serverId", id);
                getPlugin().saveConfig();
                log("Set server id to " + id);
            }
        } else {
            log("Discord integration is disabled by user, skipping connection to robot");
        }
    }
}
