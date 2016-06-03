package me.mrkirby153.kcuhc.discord.commands;

import com.google.common.io.ByteArrayDataOutput;

public class RemoveTeam extends DiscordCommand{

    private String teamName;

    public RemoveTeam(String teamName){
        super("removeTeam");
        this.teamName = teamName;
    }
    @Override
    public void process(ByteArrayDataOutput out) {
        out.writeUTF(teamName);
    }
}
