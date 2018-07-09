package com.mrkirby153.kcuhc.discord;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.contexts.OnlinePlayer;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import me.mrkirby153.kcutils.Chat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("discord")
public class DiscordCommand extends BaseCommand {

    private DiscordModule module;
    private UHCGame game;

    public DiscordCommand(UHCGame game, DiscordModule module) {
        this.module = module;
        this.game = game;
    }

    @Subcommand("create")
    public void createTeam(CommandSender sender, UHCTeam team) {
        this.module.createTeam(team);
        sender.sendMessage(Chat.INSTANCE
            .message("Discord", "Initializing team {team}", "{team}", team.getTeamName())
            .toLegacyText());
    }

    @Subcommand("remove")
    public void destroyTeam(CommandSender sender, UHCTeam team) {
        this.module.destroyTeam(team);
        sender.sendMessage(
            Chat.INSTANCE.message("Discord", "Destroying team {team}", "{team}", team.getTeamName())
                .toLegacyText());
    }

    @Subcommand("link")
    public void link(Player player) {
        module.playerMapper.createLink(player);
    }

    @Subcommand("forcelink")
    public void forceLink(CommandSender sender, OnlinePlayer player, String id) {
        module.playerMapper.forceLink(player.player, id);
        sender.sendMessage(Chat.INSTANCE
            .message("Discord", "Forcibly linking {player} to {id}", player.player.getName(),
                "{id}", id).toLegacyText());
    }

    @Subcommand("generate")
    public void generateTeamChannels(CommandSender sender) {
        game.getTeams().values().forEach(team -> {
            module.createTeam(team);
        });
        sender.sendMessage(
            Chat.INSTANCE.message("Discord", "Generating team channels").toLegacyText());
    }

    @Subcommand("destroy")
    public void destroyAllTeamChannels(CommandSender sender) {
        game.getTeams().values().forEach(team -> {
            module.destroyTeam(team);
        });
        sender.sendMessage(
            Chat.INSTANCE.message("Discord", "Destroying team channels").toLegacyText());
    }
}
