package me.mrkirby153.kcuhc.discord.commands;

import com.google.common.io.ByteArrayDataOutput;

public class Link extends DiscordCommand {

    private String serverId;
    private String guildId;

    public Link(String serverId, String guildId) {
        super("link");
        this.serverId = serverId;
        this.guildId = guildId;
    }

    @Override
    public void process(ByteArrayDataOutput out) {
        out.writeUTF(serverId);
        out.writeUTF(guildId);
    }
}
