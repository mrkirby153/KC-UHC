package com.mrkirby153.kcuhc.discord;

import com.google.common.base.Throwables;
import com.mrkirby153.kcuhc.discord.objects.DiscordUHCTeam;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class DiscordRobot {

    private final String apiToken;
    private final String guildId;

    private JDA jda;

    private boolean ready = false;

    private HashMap<ScoreboardTeam, DiscordUHCTeam> teams = new HashMap<>();

    private HashMap<UUID, String> linkedUsers = new HashMap<>();

    private HashMap<String, UUID> linkCodes = new HashMap<>();

    private VoiceChannel lobbyChannel = null;

    public DiscordRobot(String apiToken, String guildId) {
        this.apiToken = apiToken;
        this.guildId = guildId;
    }

    private static String generateLinkCode() {
        String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder code = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 5; i++) {
            code.append(ALLOWED_CHARS.charAt(r.nextInt(ALLOWED_CHARS.length())));
        }
        return code.toString();
    }

    /**
     * Connects to Discord
     */
    public void connect() {
        try {
            this.jda = new JDABuilder(AccountType.BOT).setToken(this.apiToken)
                    .addEventListener(new DiscordEventListener(this))
                    .setStatus(OnlineStatus.IDLE).buildAsync();
        } catch (LoginException | RateLimitedException e) {
            Throwables.propagate(e);
        }
    }

    /**
     * Creates a link code for the user
     *
     * @param uuid The user to create a link code for
     * @return The link code
     */
    public String createLinkCode(UUID uuid) {
        String code;
        this.linkCodes.put(code = generateLinkCode(), uuid);
        return code;
    }

    /**
     * Creates a team on the discord server
     *
     * @param team The team to create
     */
    public void createTeam(ScoreboardTeam team) {
        DiscordUHCTeam t = new DiscordUHCTeam(team, this);
        this.teams.put(team, t);
        t.create(r -> team.getPlayers().stream()
                .map(this::getUser)
                .filter(Objects::nonNull)
                .forEach(u -> {
                    getGuild().getController().addRolesToMember(getGuild().getMember(u), r).queue();
                    Member m = getGuild().getMember(u);
                    if (m.getVoiceState().inVoiceChannel()) {
                        // Move user into the team channel
                        VoiceChannel object = t.getVoiceChannel().getObject();
                        if (object != null)
                            getGuild().getController().moveVoiceMember(m, object).queue();
                    }
                }));
    }

    /**
     * Destroys all the teams
     */
    public void destroyAllTeams() {
        this.teams.values().forEach(DiscordUHCTeam::destroy);
        this.teams.clear();
    }

    /**
     * Destroys a team
     *
     * @param team The team to destroy
     */
    public void destroyTeam(ScoreboardTeam team) {
        if (this.teams.containsKey(team))
            this.teams.remove(team).destroy();
    }

    /**
     * Disconnects the robot from Discord
     */
    public void disconnect() {
        this.ready = false;
        this.destroyAllTeams();
        this.lobbyChannel.delete().queue();
        this.jda.shutdown();
    }

    /**
     * Gets the guild to act in
     *
     * @return The guild to act in
     */
    public Guild getGuild() {
        return this.jda.getGuildById(this.guildId);
    }

    public UUID getUUID(String code) {
        return this.linkCodes.get(code);
    }

    /**
     * Gets a {@link User} from a Minecraft UUID
     *
     * @param uuid The uuid
     * @return The
     */
    public User getUser(UUID uuid) {
        if (this.linkedUsers.containsKey(uuid)) {
            return this.jda.getUserById(this.linkedUsers.get(uuid));
        }
        return null;
    }

    public void link(User user, String code) {
        this.linkedUsers.put(this.linkCodes.remove(code), user.getId());
    }

    /**
     * Moves all the linked users to the lobby channel (which is created)
     */
    public void moveAllUsersToLobby() {
        if (this.lobbyChannel == null)
            getGuild().getController().createVoiceChannel("Lobby").queue(chan -> {
                this.lobbyChannel = (VoiceChannel) chan;
                this.linkedUsers.values().stream().map(id -> this.jda.getUserById(id))
                        .filter(Objects::nonNull).map(user -> getGuild().getMember(user)).forEach(user -> {
                    getGuild().getController().moveVoiceMember(user, (VoiceChannel) chan).queue();
                });
            });
        else
            this.linkedUsers.values().stream().map(id -> this.jda.getUserById(id)).filter(Objects::nonNull).map(user -> getGuild().getMember(user)).forEach(member -> {
                getGuild().getController().moveVoiceMember(member, this.lobbyChannel).queue();
            });
    }

    /**
     * Returns if the robot is ready
     *
     * @return True if the robot is ready
     */
    boolean isReady() {
        return ready;
    }

    /**
     * Marks the robot as ready
     */
    void setReady() {
        this.ready = true;
        this.jda.getPresence().setStatus(OnlineStatus.ONLINE);
    }
}
