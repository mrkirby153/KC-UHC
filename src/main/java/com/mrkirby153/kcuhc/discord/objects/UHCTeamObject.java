package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Consumer;

public class UHCTeamObject implements DiscordObject<UHCTeamObject> {

    private ScoreboardTeam team;
    private DiscordModule module;

    private TeamRoleObject role;
    private TextChannelObject textChannel;
    private VoiceChannelObject voiceChannel;
    private ChannelCategoryObject category;

    public UHCTeamObject(ScoreboardTeam team, DiscordModule module) {
        this.team = team;
        this.module = module;
    }


    @Override
    public void create(Consumer<UHCTeamObject> consumer) {
        Role publicRole = this.module.guild.getPublicRole();
        Role adminRole = this.module.adminRole;

        this.category = new ChannelCategoryObject(this.module, "Team " + this.team.getTeamName());
        this.role = new TeamRoleObject(this.module, this.team);
        this.category.create(category -> {
            // Let the admin role and the public role see the channel
            createOverride(category, publicRole, null, new Permission[]{Permission.VIEW_CHANNEL});
            createOverride(category, adminRole, new Permission[]{Permission.VIEW_CHANNEL}, null);

            this.role.create(role -> {
                createOverride(category, role, new Permission[]{Permission.VIEW_CHANNEL}, null);

                this.textChannel = new TextChannelObject("team " + this.team.getTeamName(),
                    this.module, category);
                this.voiceChannel = new VoiceChannelObject("Team " + this.team.getTeamName(),
                    this.module, category);

                this.textChannel.create();
                this.voiceChannel.create();
            });
        });
        if (consumer != null) {
            consumer.accept(this);
        }
    }

    @Override
    public Optional<UHCTeamObject> get() {
        return Optional.of(this);
    }

    @Override
    public void delete() {
        if (this.category != null) {
            this.category.delete();
        }
        if (this.textChannel != null) {
            this.textChannel.delete();
        }
        if (this.voiceChannel != null) {
            this.voiceChannel.delete();
        }
        if (this.role != null) {
            this.role.delete();
        }
    }

    /**
     * Adds a user to the team
     *
     * @param player The player to add
     */
    public void joinTeam(Player player) {
        User u = this.module.playerMapper.getUser(player.getUniqueId());
        if (u != null) {
            Member m = this.module.guild.getMember(u);
            if (m != null) {
                module.log(":inbox_tray:",
                    "Adding `" + u.getName() + "#" + u.getDiscriminator() + "` to team `"
                        + this.team.getTeamName() + "`");
                this.role.get().ifPresent(
                    r -> this.module.guild.addRoleToMember(m, r).queue());

                GuildVoiceState state = m.getVoiceState();
                if (state.inVoiceChannel()) {
                    this.voiceChannel.get().ifPresent(vc -> module.guild.moveVoiceMember(m, vc).queue());
                }
            }
        }
    }

    /**
     * Removes a player from the team
     *
     * @param player The player to remove
     */
    public void leaveTeam(Player player) {
        User u = this.module.playerMapper.getUser(player.getUniqueId());
        if (u != null) {
            Member m = this.module.guild.getMember(u);
            if (m != null) {
                module.log(":outbox_tray:",
                    "Removing `" + u.getName() + "#" + u.getDiscriminator() + "` from team `"
                        + this.team.getTeamName() + "`");
                this.role.get().ifPresent(
                    r -> this.module.guild.removeRoleFromMember(m, r).queue());
            }
        }
    }

    private void createOverride(GuildChannel c, Role role, Permission[] allow, Permission[] deny) {
        PermissionOverride override = c.getPermissionOverride(role);
        if (override == null) {
            PermissionOverrideAction action =
                c.createPermissionOverride(role);
            if (allow != null) {
                action = action.setAllow(allow);
            }
            if (deny != null) {
                action = action.setDeny(deny);
            }
            action.queue();
        } else {
            PermissionOverrideAction manager = override.getManager();
            if (allow != null) {
                manager = manager.grant(allow);
            }
            if (deny != null) {
                manager = manager.deny(deny);
            }
            manager.queue();
        }
    }
}
