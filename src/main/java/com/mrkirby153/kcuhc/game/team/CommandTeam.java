package com.mrkirby153.kcuhc.game.team;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.contexts.OnlinePlayer;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("team")
public class CommandTeam extends BaseCommand {

    private final UHC uhc;

    public CommandTeam(UHC uhc) {
        this.uhc = uhc;
    }


    @Subcommand("create")
    public void createTeam(CommandSender sender, String name, ChatColor color) {
        uhc.getGame().createTeam(name, color);
        sender.sendMessage(C.m("Team", "Created team {team}!", "{team}", name).toLegacyText());
    }

    @Subcommand("delete")
    @CommandCompletion("@teams")
    public void deleteTeam(CommandSender sender, UHCTeam team) {
        uhc.getGame().deleteTeam(team);
        sender.sendMessage(C.m("Team", "Team {team} removed!", "{team}", team.getTeamName()).toLegacyText());
    }

    @Subcommand("list")
    @CommandAlias("teams")
    public void getTeams(CommandSender sender) {
        StringBuilder teams = new StringBuilder();
        uhc.getGame().getTeams().keySet().forEach(s -> {
            teams.append(s);
            teams.append(", ");
        });
        String s = teams.toString();
        String substring = s.substring(0, Math.max(0, s.length() - 2));
        sender.sendMessage(C.m("Team", "Teams: {teams}", "{teams}", substring).toLegacyText());
    }

    @Subcommand("join")
    @CommandCompletion("@teams @players")
    public void joinTeam(Player sender, UHCTeam team, @Optional OnlinePlayer player) {
        if(player != null){
            team.addPlayer(player.player);
            player.player.spigot().sendMessage(C.m("Team", "You have been assigned to {team} by {assignee}",
                    "{team}", team.getTeamName(), "{assignee}", sender.getName()));
            sender.sendMessage(C.m("Team", "Assigned {player} to {team}",
                    "{player}", player.player.getName(), "{team}", team.getTeamName()).toLegacyText());
        } else {
            team.addPlayer(sender);
            sender.spigot().sendMessage(C.m("Team", "You have joined team {team}", "{team}", team.getTeamName()));
        }
    }

    @Subcommand("leave")
    public void leaveTeam(Player sender) {
        ScoreboardTeam currentTeam = uhc.getGame().getTeam(sender);
        if (uhc.getGame().getCurrentState() == GameState.ALIVE) {
            sender.spigot().sendMessage(C.e("You cannot change teams while the game is running!"));
            return;
        }
        if (currentTeam == null) {
            sender.spigot().sendMessage(C.e("You are not on a team!"));
            return;
        }
        currentTeam.removePlayer(sender);
        sender.spigot().sendMessage(C.m("Team", "You have left team {team}", "{team}", currentTeam.getTeamName()));
    }

    @Default
    public void showTeam(Player sender) {
        ScoreboardTeam team = uhc.getGame().getTeam(sender);
        if (team == null) {
            sender.spigot().sendMessage(C.m("Team", "You are not on a team!"));
            return;
        }
        sender.spigot().sendMessage(C.m("Team", "You are on team {team}", "{team}", team.getTeamName()));
    }
}
