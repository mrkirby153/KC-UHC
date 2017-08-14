package com.mrkirby153.kcuhc.discord;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.contexts.OnlinePlayer;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.msc.DiscordModule;
import me.mrkirby153.kcutils.C;
import net.dv8tion.jda.core.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@CommandAlias("discord")
public class CommandDiscord extends BaseCommand {

    private UHCGame game;

    @Inject
    public CommandDiscord(UHCGame game) {
        this.game = game;
    }

    @Subcommand("createTeamChannels")
    public void createTeamChannels(CommandSender sender) {
        ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(m -> {
            sender.sendMessage(C.m("Discord", "Team channels creating...").toLegacyText());
            game.getTeams().values().forEach(t -> m.getRobot().createTeam(t));
        });

    }

    @Subcommand("destroy")
    public void destroy(CommandSender sender) {
        ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(m -> {
            sender.sendMessage(C.m("Discord", "Teams being destroyed").toLegacyText());
            m.getRobot().destroyAllTeams();
        });
    }

    @Subcommand("forcelink")
    @CommandCompletion("@players")
    public void forceLink(Player sender, OnlinePlayer player, String id) {
        ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(m -> {
            User u = m.getRobot().forceLink(player.player.getUniqueId(), id);
            if (u != null) {
                String account = u.getName() + "#" + u.getDiscriminator();
                sender.spigot().sendMessage(C.m("Discord", "You have linked {player} to the account {acc}",
                        "{player}", player.player.getName(), "{acc}", account));
                player.player.spigot().sendMessage(C.m("Discord", "Your account has been linked to {acc} by {player}",
                        "{acc}", account, "{player}", sender.getName()));
            } else {
                sender.spigot().sendMessage(C.m("Discord", "Could not find a user with the ID {id}", "{id}", id));
            }
        });
    }

    @Subcommand("link")
    public void link(Player player) {
        ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(m -> {
            String code = m.getRobot().createLinkCode(player.getUniqueId());

            player.sendMessage(C.m("Discord", "To link your minecraft account to discord, run this command on the discord server: {command}",
                    "{command}", "!uhcbot link " + code).toLegacyText());
        });
    }

    @Subcommand("linked")
    public void linked(Player sender) {
        ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(m -> {
            List<UUID> linked = new ArrayList<>();
            m.getRobot().getLinkedUsers().forEach((uuid, user) -> {
                Player player = Bukkit.getPlayer(uuid);
                User u = m.getRobot().getJda().getUserById(user);
                if (u != null && player != null) {
                    sender.spigot().sendMessage(C.m(player.getName(), "{user}", "{user}", u.getName() + "#" + u.getDiscriminator()));
                }
                linked.add(uuid);
            });

            List<UUID> unlinked = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Entity::getUniqueId).collect(Collectors.toList()));
            unlinked.removeIf(linked::contains);
            unlinked.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> {
                sender.spigot().sendMessage(C.m(p.getName(), "UNLINKED!"));
            });
        });
    }

    @Subcommand("update")
    @CommandCompletion("@players")
    public void update(CommandSender sender, OnlinePlayer player) {
        ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(m -> {
            m.getRobot().updateUserTeams(player.getPlayer());
            sender.sendMessage(C.m("Discord", "Updating teams for {player}", "{player}", player.getPlayer().getName()).toLegacyText());
        });
    }
}
