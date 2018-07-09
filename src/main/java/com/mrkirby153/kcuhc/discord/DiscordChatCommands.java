package com.mrkirby153.kcuhc.discord;

import com.mrkirby153.botcore.command.Command;
import com.mrkirby153.botcore.command.Context;
import com.mrkirby153.botcore.command.args.CommandContext;

public class DiscordChatCommands {

    @Command(name = "ping", clearance = 100, parent = "uhcbot")
    public void ping(Context context, CommandContext commandContext) {
        context.getChannel().sendMessage("Pong!").queue();
    }
}
