package com.mrkirby153.kcuhc.discord;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.discord.mapper.DiscordOauthMapper;
import com.mrkirby153.kcuhc.discord.mapper.UHCBotLinkMapper;
import com.mrkirby153.kcuhc.discord.oauth.DiscordOAuthModule;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcutils.Chat;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@CommandAlias("discord")
public class DiscordCommand extends BaseCommand {

    private DiscordModule module;
    private UHCGame game;
    private UHC uhc;

    public DiscordCommand(UHC uhc, UHCGame game, DiscordModule module) {
        this.module = module;
        this.game = game;
        this.uhc = uhc;
    }

    @Subcommand("create")
    @CommandPermission("kcuhc.discord.create")
    public void createTeam(CommandSender sender, UHCTeam team) {
        this.module.createTeam(team);
        sender.sendMessage(Chat
            .message("Discord", "Initializing team {team}", "{team}", team.getTeamName())
            .toLegacyText());
    }

    @Subcommand("remove")
    @CommandPermission("kcuhc.discord.remove")
    public void destroyTeam(CommandSender sender, UHCTeam team) {
        this.module.destroyTeam(team);
        sender.sendMessage(
            Chat.message("Discord", "Destroying team {team}", "{team}", team.getTeamName())
                .toLegacyText());
    }

    @Subcommand("link")
    @CommandPermission("kcuhc.discord.link")
    public void link(Player player) {
        module.playerMapper.createLink(player);
    }

    @Subcommand("forcelink")
    @CommandCompletion("@players")
    @CommandPermission("kcuhc.discord.link.force")
    public void forceLink(CommandSender sender, OnlinePlayer player, String id) {
        module.playerMapper.forceLink(player.player, id);
        sender.sendMessage(Chat
            .message("Discord", "Forcibly linking {player} to {id}", "{player}",
                player.player.getName(),
                "{id}", id).toLegacyText());
    }

    @Subcommand("generate")
    @CommandPermission("kcuhc.discord.generate")
    public void generateTeamChannels(CommandSender sender) {
        this.module.createChannels();
        sender.sendMessage(
            Chat.message("Discord", "Generating team channels").toLegacyText());
    }

    @Subcommand("destroy")
    @CommandPermission("kcuhc.discord.remove.all")
    public void destroyAllTeamChannels(CommandSender sender) {
        this.module.destroyChannels();
        sender.sendMessage(
            Chat.message("Discord", "Destroying team channels").toLegacyText());
    }

    @Subcommand("distribute")
    @CommandPermission("kcuhc.discord.distribute")
    public void distributePlayers(CommandSender sender) {
        if (!this.module.ready) {
            sender.sendMessage(Chat
                .error("Cannot distribute players. Team channels have not been created")
                .toString());
            return;
        }
        this.module.distributeUsers();
    }

    @Subcommand("linked")
    @CommandPermission("kcuhc.discord.linked")
    public void linkedPlayers(CommandSender sender) {
        HashMap<UUID, String> linkedUsers = new HashMap<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            User u = this.module.playerMapper.getUser(player.getUniqueId());
            if (u != null) {
                linkedUsers.put(player.getUniqueId(), u.getName() + "#" + u.getDiscriminator());
            } else {
                linkedUsers.put(player.getUniqueId(), "Unlinked");
            }
        });
        sender.sendMessage(
            Chat.message("Discord", "The following users are linked:").toLegacyText());
        linkedUsers.forEach((uuid, name) -> {
            Player p = Bukkit.getPlayer(uuid);
            sender.sendMessage(
                Chat.message(p.getName(), "{account}", "{account}", name).toLegacyText());
        });
    }

    @Subcommand("backend")
    @CommandPermission("kcuhc.discord.backend")
    public void setLinkBackend(CommandSender sender, LinkBackend backend) {
        switch (backend) {
            case OAUTH:
                Optional<DiscordOAuthModule> mod = ModuleRegistry.INSTANCE.getLoadedModule(
                    DiscordOAuthModule.class);
                if (mod.isPresent()) {
                    module.playerMapper = new DiscordOauthMapper(mod.get(), module.jda);
                    sender.sendMessage(
                        Chat.message("Discord", "Set backend to OAuth").toLegacyText());
                } else {
                    sender.sendMessage(
                        Chat.error("Discord Oauth module not loaded!").toLegacyText());
                }
                break;
            case CODE:
                module.playerMapper = new UHCBotLinkMapper(uhc, module);
                sender.sendMessage(
                    Chat.message("Discord", "Set backend to link code").toLegacyText());
                break;
        }
    }
}
