package com.mrkirby153.kcuhc.discord;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.msc.DiscordModule;
import me.mrkirby153.kcutils.C;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("discord")
public class CommandDiscord extends BaseCommand {

    private UHCGame game;

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
    public void destroy(CommandSender sender){
        ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(m -> {
            sender.sendMessage(C.m("Discord", "Teams being destroyed").toLegacyText());
            m.getRobot().destroyAllTeams();
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
}
