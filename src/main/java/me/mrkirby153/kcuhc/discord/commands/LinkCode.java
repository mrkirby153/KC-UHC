package me.mrkirby153.kcuhc.discord.commands;

import com.google.common.io.ByteArrayDataOutput;
import org.bukkit.entity.Player;

public class LinkCode extends DiscordCommand {


    private Player player;


    public LinkCode(Player player) {
        super("linkCode");
        this.player = player;
    }

    @Override
    public void process(ByteArrayDataOutput out) {
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(player.getName());
    }
}
