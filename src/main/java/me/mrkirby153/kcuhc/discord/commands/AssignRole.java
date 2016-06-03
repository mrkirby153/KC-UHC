package me.mrkirby153.kcuhc.discord.commands;

import com.google.common.io.ByteArrayDataOutput;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AssignRole extends DiscordCommand {

    private String uuid;

    public AssignRole(UUID player) {
        super("assignRole");
        uuid = player.toString();
    }

    public AssignRole(Player player) {
        this(player.getUniqueId());
    }

    @Override
    public void process(ByteArrayDataOutput out) {
        out.writeUTF(uuid);
    }
}
