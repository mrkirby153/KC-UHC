package me.mrkirby153.kcuhc.discord.commands;

import com.google.common.io.ByteArrayDataOutput;

public class NewTeam extends DiscordCommand{

    private String teamName;

    public NewTeam(String teamName){
        super("newTeam");
        this.teamName = teamName;
    }

    @Override
    public void process(ByteArrayDataOutput out) {
        out.writeUTF(teamName);
    }
}
