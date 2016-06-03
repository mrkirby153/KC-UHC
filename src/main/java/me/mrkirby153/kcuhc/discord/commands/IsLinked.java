package me.mrkirby153.kcuhc.discord.commands;

import com.google.common.io.ByteArrayDataOutput;
import org.bukkit.entity.Player;

import java.util.UUID;

public class IsLinked extends DiscordCommand {

    private String uuid;

    public IsLinked(UUID u) {
        super("isLinked");
        this.uuid = u.toString();
    }

    public IsLinked(Player player) {
        this(player.getUniqueId());
    }


    @Override
    public void process(ByteArrayDataOutput out) {
        out.writeUTF(uuid);
    }
}
