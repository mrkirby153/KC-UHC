package me.mrkirby153.kcuhc.discord;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.AccountManager;
import net.dv8tion.jda.managers.ChannelManager;
import net.dv8tion.jda.managers.GuildManager;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

public class DiscordHandler extends ListenerAdapter {
    private HashMap<UUID, String> playerToDiscordName = new HashMap<>();
    private HashMap<String, UUID> verificationCodes = new HashMap<>();
    private HashMap<String, DiscordTeam> teams = new HashMap<>();

    private Guild guild;
    private GuildManager manager;
    private String guildName;
    private ChannelManager linkChannel;
    private ChannelManager lobbyVoiceChannel;
    private ChannelManager logChannel;

    public DiscordHandler(String guild) {
        this.guildName = guild;
    }

    public void init() {
        this.guild = UHC.jda.getGuildsByName(guildName).get(0);
        manager = new GuildManager(guild);
        linkChannel = guild.createTextChannel("uhc-link");
        linkChannel.setTopic("Use /discord link ingame and then come back here");
        linkChannel.update();
        lobbyVoiceChannel = guild.createVoiceChannel("Lobby");
        initLoggingChannel();
    }

    @Override
    public void onReady(ReadyEvent event) {
        UHC.jda = event.getJDA();
        AccountManager m = UHC.jda.getAccountManager();
        m.setGame("Minecraft UHC");
        m.setUsername("KC UHC Bot");
        m.update();
        init();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getGuild() != guild) {
            return;
        }
        if (!event.getMessage().getContent().startsWith("!"))
            return;
        String[] fullArgs = event.getMessage().getContent().split(" ");
        String[] args = new String[0];
        String command = fullArgs[0].substring(1);
        if (fullArgs.length > 1) {
            args = new String[fullArgs.length - 1];
            System.arraycopy(fullArgs, 1, args, 0, args.length);
        }
        if (onCommand(event.getAuthor().getUsername(), event.getChannel(), command, args)) {
            event.getMessage().deleteMessage();
        }
    }


    private boolean onCommand(String sender, MessageChannel channel, String command, String[] args) {
        if (command.equalsIgnoreCase("link")) {
            if (channel != linkChannel.getChannel()) {
                return false;
            }
            String id = args[0];
            UUID owner = verificationCodes.remove(id);
            if (owner == null) {
                channel.sendMessage(sender + ", Invalid code!");
                return true;
            } else {
                playerToDiscordName.put(owner, sender);
                User userByName = getUserByName(sender);
                String mention;
                if (userByName == null) {
                    mention = sender;
                } else {
                    mention = userByName.getAsMention();
                }
                log("Linking " + sender + " to MC name " + Bukkit.getOfflinePlayer(owner).getName());
                channel.sendMessage(mention + ", You have linked your discord account to minecraft name: " + Bukkit.getOfflinePlayer(owner).getName());
                return true;
            }
        }
        return false;
    }

    public void bringEveryoneToLobby() {
        log("Bringing everyone to the lobby");
        for (User u : guild.getUsers()) {
            if (connectedToVoiceChannel(u)) {
                log("Moving " + u.getUsername() + " to the lobby");
                manager.moveVoiceUser(u, (VoiceChannel) lobbyVoiceChannel.getChannel());
            }
            for (Role r : guild.getRolesForUser(u)) {
                manager.removeRoleFromUser(u, r);
            }
            manager.addRoleToUser(u, guild.getPublicRole());
            manager.update();
        }
    }

    public void sendEveryoneToChannels() {
        log("Sending teams to channels");
        for (Player p : UHC.arena.players()) {
            joinTeamChannel(p);
        }
    }

    public void joinTeamChannel(Player p) {
        UHCTeam team = TeamHandler.getTeamForPlayer(p);
        if (team != null) {
            DiscordTeam dTeam = teams.get(team.getName());
            if (dTeam == null)
                return;
            User discordUser = getUserByName(playerToDiscordName.get(p.getUniqueId()));
            if (discordUser == null)
                return;
            if (!connectedToVoiceChannel(discordUser))
                return;
            VoiceChannel ch = (VoiceChannel) dTeam.voiceChannel.getChannel();
            manager.addRoleToUser(discordUser, dTeam.role.getRole());
            manager.update();
            manager.moveVoiceUser(discordUser, ch);
            log("Moved " + p.getName() + " to " + ch.getName());
        }
    }

    public void removeTeamRole(Player p) {
        UHCTeam team = TeamHandler.getTeamForPlayer(p);
        DiscordTeam dTeam = teams.get(team.getName());
        if (dTeam == null)
            return;
        User discordUser = getUserByName(playerToDiscordName.get(p.getUniqueId()));
        if (discordUser == null)
            return;
        if (!connectedToVoiceChannel(discordUser))
            return;
        manager.removeRoleFromUser(discordUser, dTeam.role.getRole());
        manager.update();
    }

    public void initChannels() {
        for (UHCTeam team : TeamHandler.teams()) {
            DiscordTeam discordTeam = new DiscordTeam(guild, team.getName(), Character.toString(getCode(team.getColor())));
            discordTeam.create();
            log("Created channel for team " + team.getName());
            this.teams.put(team.getName(), discordTeam);
        }
    }

    public void cleanupTeams() {
        log("Destroying team channels");
        Iterator<DiscordTeam> teamIterator = this.teams.values().iterator();
        while (teamIterator.hasNext()) {
            teamIterator.next().destroy();
            teamIterator.remove();
        }
        this.teams = new HashMap<>();
    }

    private static char getCode(net.md_5.bungee.api.ChatColor color) {
        try {
            Field f = color.getClass().getDeclaredField("code");
            f.setAccessible(true);
            return (char) f.get(color);
        } catch (Exception e) {
            return 'f';
        }
    }

    public void setLinkCode(String code, UUID uuid) {
        this.verificationCodes.put(code, uuid);
    }

    public boolean hasLinkedDiscord(UUID u) {
        return playerToDiscordName.containsKey(u);
    }

    public ArrayList<UUID> getAllLinkedPlayers() {
        ArrayList<UUID> toReturn = new ArrayList<>();
        for (Map.Entry<UUID, String> e : playerToDiscordName.entrySet()) {
            toReturn.add(e.getKey());
        }
        return toReturn;
    }

    public void shutdown() {
        VoiceChannel generalChannel = generalExists();
        if (generalChannel == null) {
            generalChannel = (VoiceChannel) guild.createVoiceChannel("General").getChannel();
        }
        for (User u : guild.getUsers()) {
            if (connectedToVoiceChannel(u))
                manager.moveVoiceUser(u, generalChannel);
        }
        linkChannel.delete();
        lobbyVoiceChannel.delete();
        logChannel.delete();
        cleanupTeams();
    }

    private VoiceChannel generalExists() {
        for (VoiceChannel m : guild.getVoiceChannels()) {
            if (m.getName().equalsIgnoreCase("general")) {
                return m;
            }
        }
        return null;
    }

    private User getUserByName(String name) {
        for (User u : guild.getUsers()) {
            if (u.getUsername().equalsIgnoreCase(name))
                return u;
        }
        return null;
    }

    private boolean connectedToVoiceChannel(User user) {
        for (VoiceChannel m : guild.getVoiceChannels()) {
            if (m.getUsers().contains(user))
                return true;
        }
        return false;
    }

    private void initLoggingChannel() {
        logChannel = guild.createTextChannel("kc-uhc-logs");
        PermissionOverrideManager o = logChannel.getChannel().createPermissionOverride(guild.getPublicRole());
        o.deny(Permission.MESSAGE_READ);
        o.update();
        log("Logging initialized!");
    }

    private void log(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("YYY-MM-dd HH:mm:ss");
        ((MessageChannel) logChannel.getChannel()).sendMessage("[" + sdf.format(System.currentTimeMillis()) + "] " + message);
    }
}
