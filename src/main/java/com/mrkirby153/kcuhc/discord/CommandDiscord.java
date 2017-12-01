package com.mrkirby153.kcuhc.discord;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import me.mrkirby153.kcutils.Chat;
import net.dv8tion.jda.core.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

@CommandAlias("discord")
public class CommandDiscord extends BaseCommand {

    private DiscordModule module;

    public CommandDiscord(DiscordModule module) {
        this.module = module;
    }

    @Subcommand("link")
    public void link(Player player) {
        module.getMapper().createLink(player);
    }

    @Subcommand("linked")
    public void linked(Player sender) {
        ArrayList<UUID> linked = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            User user = module.getMapper().getUser(player.getUniqueId());
            if (user != null) {
                sender.spigot().sendMessage(Chat.INSTANCE
                    .message(player.getName(), "{user}", "{user}",
                        user.getName() + "#" + user.getDiscriminator()));
                linked.add(player.getUniqueId());
            }
        });

        Bukkit.getOnlinePlayers().stream().map(Entity::getUniqueId).filter(u -> !linked.contains(u))
            .map(Bukkit::getPlayer).filter(
            Objects::nonNull).forEach(
            p -> sender.spigot().sendMessage(Chat.INSTANCE.message(p.getName(), "Unlinked!")));
    }
}
