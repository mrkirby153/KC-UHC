package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.Role;

import java.util.function.Consumer;

public class UHCTeamObject extends DiscordObject<UHCTeamObject> {

    private ScoreboardTeam team;

    private TeamRoleObject role;
    private TextChannelObject textChannel;
    private VoiceChannelObject voiceChannel;
    private ChannelCategoryObject channelCategory;

    public UHCTeamObject(DiscordModule bot, ScoreboardTeam team) {
        super(bot);
        this.team = team;
    }

    @Override
    public void create(Consumer<UHCTeamObject> callback) {
        Role publicRole = this.bot.getGuild().getPublicRole();
        this.role = new TeamRoleObject(this.bot, this.team);

        this.channelCategory = new ChannelCategoryObject(this.bot,
            "Team " + this.team.getTeamName());

        this.role.create(role -> {
            this.channelCategory.create(cat -> {
                this.textChannel = new TextChannelObject(this.bot,
                    "team " + this.team.getTeamName());

                this.textChannel.create(textChan -> {
                    textChan.getManager().setParent(cat).queue();
                    // Set permissions for the public role
                    PermissionOverride defaultRole = textChan.getPermissionOverride(publicRole);
                    if (defaultRole == null) {
                        textChan.createPermissionOverride(publicRole)
                            .setDeny(Permission.MESSAGE_READ)
                            .queue();
                    } else {
                        defaultRole.getManager().deny(Permission.MESSAGE_READ).queue();
                    }
                    // Set permissions for the team role
                    PermissionOverride teamRole = textChan.getPermissionOverride(role);
                    if (teamRole == null) {
                        textChan.createPermissionOverride(role).setAllow(Permission.MESSAGE_READ)
                            .queue();
                    } else {
                        teamRole.getManager().grant(Permission.MESSAGE_READ).queue();
                    }
                });

                this.voiceChannel = new VoiceChannelObject(this.bot,
                    "Team " + this.team.getTeamName());
                this.voiceChannel.create(voiceChan -> {
                    voiceChan.getManager().setParent(cat).queue();
                    // Set permission for public
                    PermissionOverride defaultRole = voiceChan.getPermissionOverride(publicRole);
                    if (defaultRole == null) {
                        voiceChan.createPermissionOverride(role).setDeny(Permission.VOICE_CONNECT)
                            .queue();
                    } else {
                        defaultRole.getManager().deny(Permission.VOICE_CONNECT).queue();
                    }

                    PermissionOverride teamRole = voiceChan.getPermissionOverride(role);
                    if (teamRole == null) {
                        voiceChan.createPermissionOverride(role).setAllow(Permission.VOICE_CONNECT)
                            .queue();
                    } else {
                        teamRole.getManager().grant(Permission.VOICE_CONNECT).queue();
                    }
                });
                if (callback != null) {
                    callback.accept(this);
                }
            });
        });
    }

    public TeamRoleObject getRole() {
        return role;
    }

    public TextChannelObject getTextChannel() {
        return textChannel;
    }

    public VoiceChannelObject getVoiceChannel() {
        return voiceChannel;
    }

    @Override
    public void delete() {
        if (this.role != null) {
            this.role.delete();
        }
        if (this.textChannel != null) {
            this.textChannel.delete();
        }
        if (this.voiceChannel != null) {
            this.voiceChannel.delete();
        }
        if (this.channelCategory != null) {
            this.channelCategory.delete();
        }
    }
}
