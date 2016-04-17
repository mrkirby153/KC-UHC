package me.mrkirby153.kcuhc.discord;

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.managers.ChannelManager;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.managers.RoleManager;

import java.awt.*;

public class DiscordTeam {
    public ChannelManager voiceChannel;
    public ChannelManager chatChannel;
    public RoleManager role;

    private Guild guild;

    public String name;
    public String friendlyName;
    private Color color;

    public DiscordTeam(Guild guild, String teamName, String color) {
        this.name = teamName;
        this.guild = guild;
        this.color = getColor(color);
        this.friendlyName = teamName.toLowerCase().replace('_', ' ');
    }

    public void create() {
        voiceChannel = guild.createVoiceChannel("Team " + friendlyName);
        chatChannel = guild.createTextChannel("team-" + friendlyName.replace(' ', '-'));
        role = guild.createRole();
        role.setName(friendlyName);
        role.setColor(color);
        role.give(Permission.VOICE_CONNECT, Permission.VOICE_USE_VAD);
        role.update();
        // Voice channel
        PermissionOverrideManager roleOverride = voiceChannel.getChannel().createPermissionOverride(role.getRole());
        PermissionOverrideManager everyoneOverride = voiceChannel.getChannel().createPermissionOverride(guild.getPublicRole());
        everyoneOverride.deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
        roleOverride.grant(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
        everyoneOverride.update();
        roleOverride.update();
        // Text channel
        PermissionOverrideManager textEveryone = chatChannel.getChannel().createPermissionOverride(guild.getPublicRole());
        PermissionOverrideManager textOverride = chatChannel.getChannel().createPermissionOverride(role.getRole());
        textEveryone.deny(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
        textOverride.grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
        textEveryone.update();
        textOverride.update();
    }

    public void destroy() {
        role.delete();
        voiceChannel.delete();
        chatChannel.delete();
    }

    private Color getColor(String color) {
        color = color.replaceAll("\\u00A7", "").toLowerCase();
        switch (color) {
            case "0":
                return Color.BLACK;
            case "9":
            case "1":
                return Color.BLUE;
            case "2":
            case "a":
                return Color.GREEN;
            case "3":
            case "b":
                return Color.CYAN;
            case "4":
            case "c":
                return Color.RED;
            case "5":
                return Color.MAGENTA;
            case "6":
                return Color.ORANGE;
            case "7":
                return Color.GRAY;
            case "8":
                return Color.DARK_GRAY;
            case "d":
                return Color.PINK;
            case "e":
                return Color.YELLOW;
            case "f":
                return Color.WHITE;
            default:
                return Color.WHITE;
        }
    }
}
