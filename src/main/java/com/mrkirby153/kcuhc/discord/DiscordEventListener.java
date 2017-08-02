package com.mrkirby153.kcuhc.discord;

import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import java.util.UUID;

public class DiscordEventListener extends ListenerAdapter {

    private final DiscordRobot robot;

    public DiscordEventListener(DiscordRobot robot) {
        this.robot = robot;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!robot.isReady())
            return;
        if (!event.getMessage().getContent().startsWith("!uhcbot")) {
            return;
        }
        String[] split = event.getMessage().getContent().split(" ");

        String[] args = new String[split.length - 1];
        System.arraycopy(split, 1, args, 0, args.length);

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("link")) {
                if (args.length < 2) {
                    event.getMessage().delete().queue();
                    event.getChannel().sendMessage("Please specify a link code.").queue();
                    return;
                }
                String code = args[1];
                UUID u = robot.getUUID(code);
                if (u == null) {
                    event.getMessage().delete().queue();
                    event.getChannel().sendMessage("That code is invalid!").queue();
                    return;
                }
                robot.link(event.getAuthor(), code);
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("You have linked your discord account to the minecraft account `" + Bukkit.getOfflinePlayer(u).getName() + "`!").queue();
            }
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Discord robot ready!");
        robot.setReady();
    }


}
