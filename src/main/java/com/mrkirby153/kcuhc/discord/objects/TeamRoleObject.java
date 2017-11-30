package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.discord.ObjectRegistry;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.managers.RoleManager;
import org.apache.commons.lang.WordUtils;

import java.util.function.Consumer;

public class TeamRoleObject extends DiscordObject<Role> {

    private ScoreboardTeam team;

    public TeamRoleObject(DiscordModule bot, ScoreboardTeam team) {
        super(bot);
        this.team = team;
    }

    @Override
    public void create(Consumer<Role> callback) {
        bot.getGuild().getController().createRole().queue(role -> {
            set(role);
            ObjectRegistry.INSTANCE.registerForDelete(this);
            RoleManager manager = role.getManager();
            manager.setName(WordUtils.capitalizeFully("Team " + team.getTeamName())).queue(i -> {
                if (callback != null) {
                    callback.accept(role);
                }
            });
        });
    }

    @Override
    public void delete() {
        get().ifPresent(r -> r.delete().queue());
        set(null);
    }
}
