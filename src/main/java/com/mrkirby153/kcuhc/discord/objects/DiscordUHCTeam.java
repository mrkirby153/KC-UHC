package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordRobot;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.Role;

import java.util.function.Consumer;

public class DiscordUHCTeam {

    private ScoreboardTeam team;

    private DiscordRobot robot;

    private TeamRoleObject teamRole;
    private TextChannelObject textChannel;
    private VoiceChannelObject voiceChannel;

    public DiscordUHCTeam(ScoreboardTeam team, DiscordRobot robot) {
        this.team = team;
        this.robot = robot;
    }

    public void create(Consumer<Role> consumer) {
        teamRole = new TeamRoleObject(this.robot, team);

        teamRole.create(role -> {
            textChannel = new TextChannelObject(robot,
                "team-" + team.getTeamName().replace(' ', '-'));
            textChannel.create(textChannel -> {
                PermissionOverride defaultRole = textChannel
                    .getPermissionOverride(this.robot.getGuild().getPublicRole());
                if (defaultRole == null) {
                    textChannel.createPermissionOverride(this.robot.getGuild().getPublicRole())
                        .setDeny(Permission.MESSAGE_READ).queue();
                } else {
                    defaultRole.getManager().deny(Permission.MESSAGE_READ).queue();
                }

                PermissionOverride teamRole = textChannel.getPermissionOverride(role);
                if (teamRole == null) {
                    textChannel.createPermissionOverride(role).setAllow(Permission.MESSAGE_READ)
                        .queue();
                } else {
                    teamRole.getManager().grant(Permission.MESSAGE_READ).queue();
                }
            });

            voiceChannel = new VoiceChannelObject(robot, "Team " + team.getTeamName());
            voiceChannel.create(voiceChannel -> {
                PermissionOverride defaultRole = voiceChannel
                    .getPermissionOverride(this.robot.getGuild().getPublicRole());
                if (defaultRole == null) {
                    voiceChannel.createPermissionOverride(this.robot.getGuild().getPublicRole())
                        .setDeny(Permission.VOICE_CONNECT).queue();
                } else {
                    defaultRole.getManager().deny(Permission.VOICE_CONNECT).queue();
                }

                PermissionOverride teamRole = voiceChannel.getPermissionOverride(role);
                if (teamRole == null) {
                    voiceChannel.createPermissionOverride(role).setAllow(Permission.VOICE_CONNECT)
                        .queue();
                } else {
                    teamRole.getManager().grant(Permission.VOICE_CONNECT).queue();
                }
            });
            if (consumer != null) {
                consumer.accept(role);
            }
        });
    }

    public void destroy() {
        this.teamRole.delete();
        this.textChannel.delete();
        this.voiceChannel.delete();
    }

    public TeamRoleObject getTeamRole() {
        return teamRole;
    }

    public TextChannelObject getTextChannel() {
        return textChannel;
    }

    public VoiceChannelObject getVoiceChannel() {
        return voiceChannel;
    }
}
