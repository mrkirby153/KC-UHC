package com.mrkirby153.kcuhc.discord;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.discord.mapper.PlayerMapper;
import com.mrkirby153.kcuhc.discord.mapper.UHCBotLinkMapper;
import com.mrkirby153.kcuhc.discord.objects.UHCTeamObject;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.Chat;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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

    /**
     * Maps the minecraft players to their discord users
     */
    private PlayerMapper mapper;

    private CommandDiscord command;

    /**
     * If the teams have already been created and everyone assigned
     */
    private boolean created;
    private Map<UHCTeam, UHCTeamObject> teams = new HashMap<>();

    private int nagTaskId;

    @Inject
    public DiscordModule(UHC uhc) {
        super("Discord", "Integrate Discord with the game", Material.NOTE_BLOCK);
        this.uhc = uhc;
        this.command = new CommandDiscord(this);
    }

    @Override
    public void onLoad() {
        this.loadConfiguration();
        uhc.getServer().getScheduler().runTaskAsynchronously(uhc, () -> {
            try {
                this.jda = new JDABuilder(AccountType.BOT).setToken(this.apiToken)
                    .addEventListener(new EventListener()).setStatus(OnlineStatus.IDLE)
                    .buildAsync();
            } catch (LoginException e) {
                e.printStackTrace();
                this.uhc.getLogger().severe("[DISCORD] An error occurred when logging in");
            } catch (RateLimitedException e) {
                Throwables.throwIfUnchecked(e);
            }
        });
        UHC.getCommandManager().registerCommand(command);
        this.nagTaskId = this.uhc.getServer().getScheduler()
            .scheduleSyncRepeatingTask(this.uhc, () -> {
                if (this.uhc.getGame().getCurrentState() == GameState.WAITING
                    && this.getMapper() != null) {
                    Bukkit.getOnlinePlayers().stream()
                        .filter(p -> this.getMapper().getUser(p.getUniqueId()) == null)
                        .forEach(p -> {
                            p.spigot().sendMessage(Chat.INSTANCE.message("Discord",
                                "Your discord and minecraft account aren't linked. Type {command} to begin the process",
                                "{command}", "/discord link"));
                            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG,
                                SoundCategory.MASTER, 1F, 1F);
                        });
                }
            }, 0L, 600L);
    }

    @Override
    public void onUnload() {
        if (this.jda != null) {
            this.jda.shutdown();
            this.jda = null;
        }
        UHC.getCommandManager().unregisterCommand(command);
        ObjectRegistry.INSTANCE.delete();
        this.uhc.getServer().getScheduler().cancelTask(this.nagTaskId);
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
     * Gets the PlayerMapper currently in use to link a {@link java.util.UUID} to a {@link
     * net.dv8tion.jda.core.entities.User}
     *
     * @return The mapper
     */
    public PlayerMapper getMapper() {
        return mapper;
    }

    /**
     * Create teams and move everyone
     */
    public void createTeams() {
        if (this.created) {
            return;
        }
        this.uhc.getGame().getTeams().forEach((name, team) -> {
            UHCTeamObject obj = new UHCTeamObject(this, team);
            this.teams.put(team, obj);
            new JoinTeamRunnable(this.uhc, team, obj, this);
            obj.create();
        });
        this.created = true;
    }

    /**
     * Removes all the teams
     */
    public void remove() {
        this.teams.forEach((team, obj) -> obj.delete());
        this.teams.clear();
        this.created = false;
    }

    /**
     * Moves all online members from their channels into the lobby channel. <br/> If the lobby
     * channel does not exist, it will be created
     *
     * @param callback A callback called when everyone has been moved
     */
    public void moveEveryoneToLobby(Consumer<Void> callback) {
        VoiceChannel lobbyChan = getLobbyChannel();
        if (lobbyChan == null) {
            lobbyChan = (VoiceChannel) this.getGuild().getController().createVoiceChannel("Lobby")
                .complete();
        }
        List<Member> members = this.getGuild().getMembers().stream()
            .filter(m -> m.getVoiceState().inVoiceChannel()).collect(
                Collectors.toList());
        for (int i = 0; i < members.size(); i++) {
            int finalI = i;
            this.getGuild().getController().moveVoiceMember(members.get(i), lobbyChan).queue(v -> {
                if (finalI >= members.size() - 1) {
                    if (callback != null) {
                        callback.accept(null);
                    }
                }
            });
        }
    }

    /**
     * Attempts to find a lobby/general channel
     *
     * @return The channel, or null if one doesn't exist
     */
    @Nullable
    public VoiceChannel getLobbyChannel() {
        VoiceChannel lobbyChan = null;
        // Attempt to find a channel named "general" or "lobby"
        for (VoiceChannel c : this.getGuild().getVoiceChannels()) {
            if (c.getName().equalsIgnoreCase("general") || c.getName().equalsIgnoreCase("lobby")) {
                lobbyChan = c;
                break;
            }
        }
        return lobbyChan;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE) {
            this.createTeams();
        }
        if (event.getTo() == GameState.ENDING) {
            this.moveEveryoneToLobby(success -> this.remove());
        }
    }

    /**
     * Gets the team's discord team object
     *
     * @param team The team
     *
     * @return The team object
     */
    public UHCTeamObject getTeamObject(UHCTeam team) {
        return this.teams.get(team);
    }

    /**
     * Load configuration files
     */
    private void loadConfiguration() {
        FileConfiguration configuration = this.uhc.getConfig();
        this.apiToken = configuration.getString("discord.apiToken");
        this.guildId = configuration.getString("discord.guild");
    }

    /**
     * Set the permissions for a channel
     *
     * @param channel The channel to set
     * @param role    The role to set the permissions
     * @param grant   The permissions to grant to the role
     * @param deny    The permissions to deny to the role
     */
    public void setPermissions(Channel channel, Role role, Permission[] grant, Permission[] deny) {
        PermissionOverride override = channel.getPermissionOverride(role);
        if (grant == null) {
            grant = new Permission[0];
        }
        if (deny == null) {
            deny = new Permission[0];
        }
        if (override == null) {
            channel.createPermissionOverride(role).setAllow(grant).setDeny(deny).queue();
        } else {
            override.getManagerUpdatable().grant(grant).deny(deny).update().queue();
        }
    }

    private class EventListener extends ListenerAdapter {

        @Override
        public void onReady(ReadyEvent event) {
            DiscordModule.this.ready = true;
            DiscordModule.this.jda.getPresence().setStatus(OnlineStatus.ONLINE);
            DiscordModule.this.uhc.getLogger()
                .info("[DISCORD] Discord robot connected and initialized successfully!");
            DiscordModule.this.mapper = new UHCBotLinkMapper(DiscordModule.this);
        }
    }
}
