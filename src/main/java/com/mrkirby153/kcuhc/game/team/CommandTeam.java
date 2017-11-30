package com.mrkirby153.kcuhc.game.team;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.contexts.OnlinePlayer;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@CommandAlias("team")
public class CommandTeam extends BaseCommand {

    private final UHC uhc;

    @Inject
    public CommandTeam(UHC uhc) {
        this.uhc = uhc;
    }


    @Subcommand("create|add")
    public void createTeam(CommandSender sender, String name, ChatColor color) {
        uhc.getGame().createTeam(name, color);
        sender.sendMessage(
            Chat.INSTANCE.message("Team", "Created team {team}!", "{team}", name).toLegacyText());
    }

    @Subcommand("delete")
    @CommandCompletion("@teams")
    public void deleteTeam(CommandSender sender, UHCTeam team) {
        uhc.getGame().deleteTeam(team);
        sender.sendMessage(
            Chat.INSTANCE.message("Team", "Team {team} removed!", "{team}", team.getTeamName())
                .toLegacyText());
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
        sender.sendMessage(
            Chat.INSTANCE.message("Team", "Teams: {teams}", "{teams}", substring).toLegacyText());
    }

    @Subcommand("join")
    @CommandCompletion("@teams @players")
    public void joinTeam(Player sender, UHCTeam team, @Optional OnlinePlayer player) {
        if (player != null) {
            if (uhc.getGame().getTeam(player.player) != null) {
                uhc.getGame().getTeam(player.player).removePlayer(player.player);
            }
            team.addPlayer(player.player);
            player.player.spigot().sendMessage(
                Chat.INSTANCE.message("Team", "You have been assigned to {team} by {assignee}",
                    "{team}", team.getTeamName(), "{assignee}", sender.getName()));
            sender.sendMessage(Chat.INSTANCE.message("Team", "Assigned {player} to {team}",
                "{player}", player.player.getName(), "{team}", team.getTeamName()).toLegacyText());
        } else {
            if (uhc.getGame().getTeam(sender) != null) {
                uhc.getGame().getTeam(sender).removePlayer(sender);
            }
            team.addPlayer(sender);
            sender.spigot().sendMessage(Chat.INSTANCE
                .message("Team", "You have joined team {team}", "{team}", team.getTeamName()));
        }
    }

    @Subcommand("leave")
    public void leaveTeam(Player sender) {
        ScoreboardTeam currentTeam = uhc.getGame().getTeam(sender);
        if (uhc.getGame().getCurrentState() == GameState.ALIVE) {
            sender.spigot().sendMessage(
                Chat.INSTANCE.error("You cannot change teams while the game is running!"));
            return;
        }
        if (currentTeam == null) {
            sender.spigot().sendMessage(Chat.INSTANCE.error("You are not on a team!"));
            return;
        }
        currentTeam.removePlayer(sender);
        sender.spigot().sendMessage(Chat.INSTANCE
            .message("Team", "You have left team {team}", "{team}", currentTeam.getTeamName()));
    }

    @Subcommand("random")
    public void randomizeTeams(Player sender, Integer teamSize) {
        List<UHCTeam> currentTeams = new ArrayList<>(uhc.getGame().getTeams().values());
        currentTeams.forEach(c -> uhc.getGame().deleteTeam(c));
        List<ChatColor> blacklistedColors = Arrays
            .asList(ChatColor.RESET, ChatColor.BOLD, ChatColor.STRIKETHROUGH, ChatColor.MAGIC,
                ChatColor.WHITE);

        List<Player> availablePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        List<ChatColor> usedColors = new ArrayList<>();

        Random random = new Random();

        int teamsRequired = (int) Math.floor(Bukkit.getOnlinePlayers().size() / teamSize);

        for (int i = 0; i < teamsRequired; i++) {
            List<Player> selectedPlayers = new ArrayList<>();

            for (int j = 0; j < teamSize; j++) {
                Player p = availablePlayers.get(random.nextInt(availablePlayers.size()));
                selectedPlayers.add(p);
                availablePlayers.remove(p);
            }

            ChatColor color;
            do {
                color = ChatColor.values()[random.nextInt(ChatColor.values().length)];
            } while (blacklistedColors.contains(color) || usedColors.contains(color));

            StringBuilder nameBuilder = new StringBuilder();
            selectedPlayers.forEach(p -> nameBuilder.append(p.getName()).append("_"));

            String name = nameBuilder.toString().trim();

            name = name.substring(0, name.length() - 1);

            UHCTeam t = uhc.getGame().createTeam(name, color);
            selectedPlayers.forEach(t::addPlayer);
            usedColors.add(color);
        }
        sender.spigot().sendMessage(
            Chat.INSTANCE.message("Team", "Created {teamSize} teams with {players} players",
                "{teamSize}", teamsRequired, "{players}", teamSize));
    }

    @Default
    public void showTeam(Player sender) {
        ScoreboardTeam team = uhc.getGame().getTeam(sender);
        if (team == null) {
            sender.spigot().sendMessage(Chat.INSTANCE.message("Team", "You are not on a team!"));
            return;
        }
        sender.spigot().sendMessage(
            Chat.INSTANCE.message("Team", "You are on team {team}", "{team}", team.getTeamName()));
    }

    @Subcommand("swap")
    @CommandCompletion("@players @players")
    public void swapTeams(Player sender, OnlinePlayer player1, OnlinePlayer player2) {
        ScoreboardTeam p1Team = uhc.getGame().getTeam(player1.player);

        ScoreboardTeam p2Team = uhc.getGame().getTeam(player2.player);

        if (p1Team != null) {
            p1Team.removePlayer(player1.player);
            p1Team.addPlayer(player2.player);
        }

        if (p2Team != null) {
            p2Team.removePlayer(player2.player);
            p2Team.addPlayer(player1.player);
        }
        sender.spigot().sendMessage(Chat.INSTANCE
            .message("Team", "Swapping the teams of {p1} and {p2}", "{p1}",
                player1.player.getName(),
                "{p2}", player2.player.getName()));
    }

    @Subcommand("twoteams")
    public void twoTeams(Player player) {
        List<UHCTeam> currentTeams = new ArrayList<>(uhc.getGame().getTeams().values());
        currentTeams.forEach(c -> uhc.getGame().deleteTeam(c));

        UHCTeam redTeam = uhc.getGame().createTeam("Red", ChatColor.RED);
        UHCTeam blueTeam = uhc.getGame().createTeam("Blue", ChatColor.BLUE);

        Random random = new Random();

        List<Player> availablePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        int playersPerTeam = (int) Math.floor(Bukkit.getOnlinePlayers().size() / 2);

        for (int i = 0; i < playersPerTeam; i++) {
            Player p = availablePlayers.get(random.nextInt(availablePlayers.size()));
            availablePlayers.remove(p);
            redTeam.addPlayer(p);
        }

        for (int i = 0; i < playersPerTeam; i++) {
            Player p = availablePlayers.get(random.nextInt(availablePlayers.size()));
            availablePlayers.remove(p);
            blueTeam.addPlayer(p);
        }

        player.spigot().sendMessage(Chat.INSTANCE
            .message("Team", "Created two teams with {players} on each team", "{players}",
                playersPerTeam));
    }
}
