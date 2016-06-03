package me.mrkirby153.kcuhc.discord.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.discord.DiscordBotConnection;

public abstract class DiscordCommand {

    private String commandName;

    public DiscordCommand(String commandName){
        this.commandName = commandName;
    }

    public abstract void process(ByteArrayDataOutput out);

    public ByteArrayDataInput send(){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(UHC.plugin.serverId());
        out.writeUTF(commandName);
        process(out);
        return DiscordBotConnection.instance.sendMessage(out.toByteArray());
    }
}
