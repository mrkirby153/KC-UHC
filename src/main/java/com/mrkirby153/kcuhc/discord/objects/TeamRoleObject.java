package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.discord.ObjectRegistry;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.lang.WordUtils;

import java.awt.Color;
import java.util.Optional;
import java.util.function.Consumer;

public class TeamRoleObject implements DiscordObject<Role> {

    private DiscordModule module;
    private ScoreboardTeam team;
    private Role role;

    public TeamRoleObject(DiscordModule module, ScoreboardTeam team) {
        this.module = module;
        this.team = team;
    }

    @Override
    public void create(Consumer<Role> consumer) {
        Color c = null;
        if (this.team instanceof UHCTeam) {
            c = new Color(((UHCTeam) this.team).toColor().asRGB());
        }
        this.module.guild.createRole()
            .setName(WordUtils.capitalizeFully("Team " + this.team.getTeamName())).setColor(c)
            .queue(r -> {
                this.role = r;
                ObjectRegistry.INSTANCE.register(this);
                if (consumer != null) {
                    consumer.accept(r);
                }
            });
    }

    @Override
    public Optional<Role> get() {
        return Optional.ofNullable(this.role);
    }

    @Override
    public void delete() {
        get().ifPresent(r -> r.delete().queue());
        this.role = null;
        ObjectRegistry.INSTANCE.unregister(this);
    }
}
