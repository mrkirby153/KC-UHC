package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

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

        this.role.create(role -> this.channelCategory.create(cat -> {
            // Set permissions for the public role
            bot.setPermissions(cat, publicRole, null, new Permission[]{Permission.VIEW_CHANNEL});
            bot.setPermissions(cat, role, new Permission[]{Permission.VIEW_CHANNEL}, null);

            this.textChannel = new TextChannelObject(this.bot, "team " + this.team.getTeamName());
            this.textChannel.create(textChan -> {
                textChan.getManager().setParent(cat).queue();
                bot.setPermissions(textChan, publicRole, null,
                    new Permission[]{Permission.VIEW_CHANNEL});
                bot.setPermissions(textChan, role, new Permission[]{Permission.VIEW_CHANNEL}, null);
            });

            this.voiceChannel = new VoiceChannelObject(this.bot, "Team " + this.team.getTeamName());
            this.voiceChannel.create(voiceChan -> {
                voiceChan.getManager().setParent(cat).queue();
                bot.setPermissions(voiceChan, publicRole, null,
                    new Permission[]{Permission.VIEW_CHANNEL});
                bot.setPermissions(voiceChan, role, new Permission[]{Permission.VIEW_CHANNEL},
                    null);
            });

            if (callback != null) {
                callback.accept(this);
            }
        }));
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

    /**
     * Adds the user to the team
     *
     * @param user     The user to add
     * @param consumer An optional callback
     */
    public void joinTeam(User user, Consumer<User> consumer) {
        this.getRole().get().ifPresent(role -> {
            Guild guild = this.bot.getGuild();
            guild.getController().addRolesToMember(guild.getMember(user), role).queue(ignored -> {
                if (consumer != null) {
                    consumer.accept(user);
                }
            });
        });
    }

    /**
     * Removes the user from the team
     *
     * @param user     The user to remove
     * @param callback An optional callback
     */
    public void leaveTeam(User user, Consumer<User> callback) {
        this.getRole().get().ifPresent(role -> {
            Guild guild = this.bot.getGuild();
            guild.getController().removeRolesFromMember(guild.getMember(user), role)
                .queue(ignored -> {
                    if (callback != null) {
                        callback.accept(user);
                    }
                });
        });
    }

    /**
     * Moves the user into the team voice channel
     *
     * @param user The user to move
     */
    public void moveToTeamChannel(User user) {
        this.getVoiceChannel().get().ifPresent(chan -> {
            Guild guild = this.bot.getGuild();
            Member member = guild.getMember(user);
            if (member.getVoiceState().inVoiceChannel()) {
                guild.getController().moveVoiceMember(member, chan).queue();
            }
        });
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

    /**
     * Checks if all parts of the team have been created
     *
     * @return True if the text and voice channel as well as the role are all created
     */
    public boolean isCreated() {
        return this.role != null && this.textChannel != null && this.voiceChannel != null
            && this.role.get().isPresent() && this.textChannel.get().isPresent()
            && this.voiceChannel.get().isPresent();
    }
}
