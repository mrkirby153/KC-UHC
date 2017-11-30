package com.mrkirby153.kcuhc.discord;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.UHCModule;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import javax.security.auth.login.LoginException;

public class DiscordModule extends UHCModule {

    private UHC uhc;

    /**
     * The Bot's API token
     */
    private String apiToken;

    /**
     * The guild to run the game on
     */
    private String guildId;

    /**
     * Ready state of the robot
     */
    private boolean ready;

    /**
     * The main JDA instance
     */
    private JDA jda;

    @Inject
    public DiscordModule(UHC uhc) {
        super("Discord Module", "Integrate Discord with the game", Material.NOTE_BLOCK);
        this.uhc = uhc;
    }

    @Override
    public void onLoad() {
        this.loadConfiguration();
        try {
            this.jda = new JDABuilder(AccountType.BOT).setToken(this.apiToken)
                .addEventListener(new EventListener()).setStatus(OnlineStatus.IDLE).buildAsync();
        } catch (LoginException e) {
            e.printStackTrace();
            this.uhc.getLogger().severe("[DISCORD] An error occurred when logging in");
        } catch (RateLimitedException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void onUnload() {
        if (this.jda != null) {
            this.jda.shutdown();
            this.jda = null;
        }
    }

    /**
     * Gets the ready state of the robot
     *
     * @return True if the robot is ready
     */
    public boolean isReady() {
        return this.ready;
    }

    /**
     * Gets the {@link JDA} instance in use
     *
     * @return The JDA instance
     */
    public JDA getJda() {
        return this.jda;
    }

    /**
     * Gets the {@link Guild} the game is running on
     *
     * @return The guild, or null
     */
    public Guild getGuild() {
        return this.jda.getGuildById(this.guildId);
    }


    /**
     * Load configuration files
     */
    private void loadConfiguration() {
        FileConfiguration configuration = this.uhc.getConfig();
        this.apiToken = configuration.getString("discord.apiToken");
        this.guildId = configuration.getString("discord.guild");
    }

    private class EventListener extends ListenerAdapter {

        @Override
        public void onReady(ReadyEvent event) {
            DiscordModule.this.ready = true;
            DiscordModule.this.jda.getPresence().setStatus(OnlineStatus.ONLINE);
            DiscordModule.this.uhc.getLogger()
                .info("[DISCORD] Discord robot connected and initialized successfully!");
        }
    }
}
