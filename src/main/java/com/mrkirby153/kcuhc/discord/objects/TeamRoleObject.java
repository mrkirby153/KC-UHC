package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordRobot;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.managers.RoleManager;
import org.apache.commons.lang.WordUtils;

import java.util.function.Consumer;

public class TeamRoleObject extends DiscordObject<Role> {

    private ScoreboardTeam team;

    public TeamRoleObject(DiscordRobot robot, ScoreboardTeam team) {
        super(robot);
        this.team = team;
    }

    @Override
    public void create(Consumer<Role> callback) {
        robot.getGuild().getController().createRole().queue(role -> {
            object = role;
            RoleManager manager = role.getManager();
            manager.setName(WordUtils.capitalizeFully("Team " + team.getTeamName())).queue(success -> {
                if(callback != null)
                    callback.accept(role);
            });
        });
    }

    @Override
    public void delete() {
        if(object != null)
            object.delete().queue();
    }
}
