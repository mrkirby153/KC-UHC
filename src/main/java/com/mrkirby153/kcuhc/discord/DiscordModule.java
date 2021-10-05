package com.mrkirby153.kcuhc.discord;

import com.google.inject.Inject;
import com.mrkirby153.botcore.command.CommandExecutor;
import com.mrkirby153.botcore.command.CommandExecutor.MentionMode;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.discord.mapper.PlayerMapper;
import com.mrkirby153.kcuhc.discord.mapper.UHCBotLinkMapper;
import com.mrkirby153.kcuhc.discord.objects.UHCTeamObject;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.event.GameStartingEvent;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardModuleManager;
import com.mrkirby153.kcuhc.scoreboard.modules.DiscordScoreboardModule;
import me.mrkirby153.kcutils.Chat;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.security.auth.login.LoginException;


public class DiscordModule extends UHCModule {

    public Guild guild;
    public Role adminRole;
    public Role spectatorRole;
    public TextChannel logChannel;
    public JDA jda;

    public PlayerMapper playerMapper;
    public boolean ready = false;
    private UHC uhc;
    private String token;
    private String guildId;
    private String adminRoleId;
    private ShardManager shardManager;
    private CommandExecutor commandExecutor;
    private DiscordChatCommands discordCommands;
    private DiscordCommand discordMinecraftCommand;
    private HashMap<UHCTeam, UHCTeamObject> teamObjectMap = new HashMap<>();


    @Inject
    public DiscordModule(UHC uhc) {
        super("Discord", "Integrate discord with the game", Material.NOTE_BLOCK);
        this.uhc = uhc;
        UHC.getCommandManager()
            .registerCommand(
                this.discordMinecraftCommand = new DiscordCommand(uhc, uhc.getGame(), this));
        playerMapper = new UHCBotLinkMapper(uhc, this);
    }

    @Override
    public void onLoad() {
        ScoreboardModuleManager.INSTANCE.installModule(new DiscordScoreboardModule(uhc.getGame(), this), -1);

        this.uhc.getLogger().info("[DISCORD] Starting up...");
        this.loadConfiguration();
        try {
            this.shardManager = DefaultShardManagerBuilder.createDefault(token).setChunkingFilter(
                ChunkingFilter.ALL).enableIntents(GatewayIntent.GUILD_MEMBERS).build();
            this.shardManager.getShards().forEach(jda -> {
                try {
                    jda.awaitReady();
                } catch (InterruptedException e) {
                    // Ignore
                }
            });
        } catch (LoginException e) {
            this.uhc.getLogger().severe("Could not load up discord module :(");
            return;
        }
        this.commandExecutor = new CommandExecutor("!", MentionMode.OPTIONAL, null,
            this.shardManager);
        this.commandExecutor.setAlertNoClearance(false);
        this.commandExecutor.setAlertUnknownCommand(false);
        this.commandExecutor.clearanceResolver = (user) -> {
            if (user.getRoles().contains(adminRole)) {
                return 100;
            }
            return 0;
        };
        this.commandExecutor.register(discordCommands = new DiscordChatCommands());
        this.commandExecutor.register(this.playerMapper);

        this.shardManager.addEventListener(new CommandEventListener());

        this.uhc.getLogger().info("Shards started up!");
        this.jda = this.shardManager.getShardById(0);

        if (!this.verifyConfiguration()) {
            this.uhc.getLogger()
                .severe("[DISCORD] Could not verify configuration, loading will stop");
            this.onUnload();
            return;
        }
        this.log(":ballot_box_with_check:", "Initialized and ready!");
    }

    @Override
    public void onUnload() {
        this.log(":no_entry:", "Module unloading");
        ObjectRegistry.INSTANCE.delete();
        this.guild = null;
        this.adminRole = null;
        this.logChannel = null;
        this.shardManager.shutdown();
        ScoreboardModuleManager.INSTANCE.removeModule(DiscordScoreboardModule.class);
    }

    private boolean verifyConfiguration() {
        try {
            this.uhc.getLogger().info("[DISCORD] Verifying configuration");
            JDA jda = this.jda;
            this.guild = jda.getGuildById(this.guildId);
            if (this.guild == null) {
                this.uhc.getLogger().warning("[DISCORD] Bot is not in guild " + this.guildId);
                return false;
            }
            this.adminRole = this.guild.getRoleById(this.adminRoleId);
            if (this.adminRole == null) {
                this.uhc.getLogger()
                    .warning("[DISCORD] Role " + this.adminRoleId + " does not exist!");
                return false;
            }
            TextChannel match = this.guild.getTextChannels().stream()
                .filter(chan -> chan.getName().equals("uhc-log")).findFirst().orElse(null);
            if (match == null) {
                this.uhc.getLogger().info("[DISCORD] Log channel doesn't exist, creating");
                this.logChannel = this.guild.createTextChannel("uhc-log")
                    .complete();
                this.uhc.getLogger().info("[DISCORD] Log channel created!");
            } else {
                this.logChannel = match;
            }
            this.uhc.getLogger().info("[DISCORD] Using text channel " + this.logChannel.getId());

            // Verify permissions
            PermissionOverride defaultOverride = this.logChannel
                .getPermissionOverride(this.guild.getPublicRole());
            if (defaultOverride == null) {
                this.logChannel.createPermissionOverride(this.guild.getPublicRole())
                    .setDeny(Permission.MESSAGE_READ).queue();
            } else {
                if (!defaultOverride.getDenied().contains(Permission.MESSAGE_READ)) {
                    defaultOverride.getManager().deny(Permission.MESSAGE_READ).queue();
                }
            }

            PermissionOverride adminRole = this.logChannel.getPermissionOverride(this.adminRole);
            if (adminRole == null) {
                this.logChannel.createPermissionOverride(this.adminRole)
                    .setAllow(Permission.MESSAGE_READ).queue();
            } else {
                if (!adminRole.getAllowed().contains(Permission.MESSAGE_READ)) {
                    adminRole.getManager().grant(Permission.MESSAGE_READ).queue();
                }
            }

            Member member = guild.getMember(this.jda.getSelfUser());
            if (!PermissionUtil.checkPermission(this.logChannel, member, Permission.MESSAGE_WRITE,
                Permission.MESSAGE_READ)) {
                this.uhc.getLogger().info("[DISCORD] Bot cannot read and write, creating override");
                PermissionOverride botOverride = this.logChannel.getPermissionOverride(member);
                if (botOverride == null) {
                    this.logChannel.createPermissionOverride(member)
                        .setAllow(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).queue();
                } else {
                    if (!botOverride.getAllowed().contains(Permission.MESSAGE_READ) || !botOverride
                        .getAllowed().contains(Permission.MESSAGE_WRITE)) {
                        botOverride.getManager()
                            .grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE).queue();
                    }
                }
            }
            this.initializeSpectatorRole();
            this.uhc.getLogger().info("[DISCORD] Configuration verified");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads the configuration for this module
     */
    private void loadConfiguration() {
        FileConfiguration cfg = this.uhc.getConfig();
        this.token = cfg.getString("discord.apiToken");
        this.guildId = cfg.getString("discord.guild");
        this.adminRoleId = cfg.getString("discord.adminRole");
    }

    /**
     * Logs a message in the bot log
     *
     * @param emoji   The emoji to include
     * @param message The message to log
     */
    public void log(String emoji, String message) {
        if (this.logChannel == null) {
            return;
        }
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis());
        this.logChannel.sendMessage(String.format("`[%s]` %s %s", timestamp, emoji, message))
            .queue();
    }

    private void initializeSpectatorRole() {
        Optional<Role> role = this.guild.getRolesByName("Spectators", true).stream().findFirst();
        if (role.isPresent()) {
            this.uhc.getLogger()
                .info("[DISCORD] Using " + role.get().getId() + " as the spectator role");
            this.spectatorRole = role.get();
        } else {
            this.spectatorRole = this.guild.createRole().setName("Spectators").complete();
        }
    }

    /**
     * Creates the team
     *
     * @param team The team to create
     */
    public void createTeam(UHCTeam team) {
        if (this.teamObjectMap.containsKey(team)) {
            return; // We've already created it
        }
        log(":shield:", "Initializing team `" + team.getTeamName() + "`");
        UHCTeamObject obj = new UHCTeamObject(team, this);
        this.teamObjectMap.put(team, obj);
        System.out.println("Created team object " + team);
        this.teamObjectMap
            .forEach((key, value1) -> System.out.println("\t - " + key + ":" + value1));
        obj.create();
    }

    /**
     * Destroys the team
     *
     * @param team The team to destroy
     */
    public void destroyTeam(UHCTeam team) {
        this.uhc.getLogger().info("[DISCORD] Destroying team " + team.getTeamName());
        UHCTeamObject obj = null;
        Iterator<Entry<UHCTeam, UHCTeamObject>> iterator = this.teamObjectMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<UHCTeam, UHCTeamObject> entry = iterator.next();
            if (entry.getKey().equals(team)) {
                iterator.remove();
                obj = entry.getValue();
                break;
            }
        }
        if (obj != null) {
            log(":wastebasket:", "Destroying team `" + team.getTeamName() + "`");
            obj.delete();
        }
        System.out.println("Removed team object " + team);
        this.teamObjectMap.keySet().forEach(k -> System.out.println("\t - " + k));
    }

    /**
     * Adds a user to the spectator role
     *
     * @param uuid The user
     */
    public void addSpectator(UUID uuid) {
        User u = playerMapper.getUser(uuid);
        if (u != null) {
            Member m = guild.getMember(u);
            if (m != null) {
                log(":crossed_swords:", "Assigning `" + u.getName() + "#" + u.getDiscriminator() +
                    "` to the spectators team");
                guild.addRoleToMember(m, this.spectatorRole).queue();
            }
        }
    }

    /**
     * Remove the users from the spectator role
     *
     * @param uuid The user
     */
    public void removeSpectator(UUID uuid) {
        User u = playerMapper.getUser(uuid);
        if (u != null) {
            Member m = guild.getMember(u);
            if (m != null) {
                log(":crossed_swords:", "Removing `" + u.getName() + "#" + u.getDiscriminator() +
                    "` from the spectators team");
                guild.removeRoleFromMember(m, this.spectatorRole).queue();
            }
        }
    }

    /**
     * Gets the {@link UHCTeamObject} corresponding to the {@link UHCTeam}
     *
     * @param team The team
     *
     * @return An optional of the object
     */
    public Optional<UHCTeamObject> getTeam(UHCTeam team) {
        for (Map.Entry<UHCTeam, UHCTeamObject> e : this.teamObjectMap.entrySet()) {
            if (e.getKey().equals(team)) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Creates all the team channels
     */
    public void createChannels() {
        if (ready) {
            return; // Don't create two at once
        }
        this.uhc.getGame().getTeams().values().forEach(this::createTeam);
        this.ready = true;
    }

    /**
     * Destroys all the team channels
     */
    public void destroyChannels() {
        if (!ready) {
            return;
        }
        this.uhc.getGame().getTeams().values().forEach(this::destroyTeam);
        this.ready = false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStarting(GameStartingEvent event) {
        if (!ready) {
            log(":warning:", "Game start aborted. Team channels have not been created");
            event.setCancelled(true);
            event.setCanceledReason("Discord team channels not generated");
        }
    }

    @EventHandler
    public void onStateChange(GameStateChangeEvent event) {
        log(":pushpin:", "Game state changing to `" + event.getTo() + "`");
        if (event.getTo() == GameState.ALIVE) {
            distributeUsers();
        }
        if (event.getTo() == GameState.ENDING) {
            bringEveryoneToLobby();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        log(":skull:", ChatColor.stripColor(event.getDeathMessage()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        User discordUser = playerMapper.getUser(event.getPlayer().getUniqueId());
        if (discordUser == null) {
            String code = playerMapper.getCode(event.getPlayer().getUniqueId());
            if (code == null) {
                // User is not linked and no code has been generated
                playerMapper.createLink(event.getPlayer());
            } else {
                event.getPlayer().sendMessage(Chat.message("Discord",
                        "To link your minecraft account to discord, run the following command in {guild}: {cmd}",
                        "{guild}", guild.getName(), "{cmd}", String.format("!uhcbot link %s", code))
                    .toLegacyText());
            }
        } else {
            event.getPlayer().sendMessage(Chat.message("Discord",
                "Your account is linked to {user}. If this is not you, re-link with {command}",
                "{user}",
                String.format("%s#%s", discordUser.getName(), discordUser.getDiscriminator()),
                "{command}", "/discord link").toLegacyText());
        }
    }

    /**
     * Sends everyone to the team channels
     */
    public void distributeUsers() {
        if (!this.ready) {
            log(":warning:", "Cannot distribute users, channels are not ready.");
            return;
        }
        log(":information_source:", "Moving everyone into team channels");
        this.teamObjectMap.forEach(
            (team, teamObject) -> team.getPlayers().stream().map(Bukkit::getPlayer)
                .filter(Objects::nonNull).forEach(teamObject::joinTeam));
    }

    private void bringEveryoneToLobby() {
        log(":information_source:", "Bringing everyone to `General`");
        List<VoiceChannel> channels = this.guild.getVoiceChannelsByName("General", true);
        VoiceChannel channel;
        if (channels.size() > 0) {
            channel = channels.get(0);
        } else {
            channel = guild.createVoiceChannel("General")
                .complete();
        }
        guild.getMembers().stream()
            .filter(m -> m.getVoiceState() != null && m.getVoiceState().inVoiceChannel())
            .forEach(m -> guild.moveVoiceMember(m, channel).queue());
    }

    private class CommandEventListener extends ListenerAdapter {

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor() == event.getGuild().getSelfMember().getUser()) {
                return; // Ignore messages from ourself
            }
            commandExecutor.execute(event.getMessage());
        }
    }
}
