package me.mrkirby153.kcuhc.discord.commands;

import com.google.common.io.ByteArrayDataOutput;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AssignSpectator extends DiscordCommand {

    private UUID u;

    public AssignSpectator(Player player) {
        super("assignSpectator");
        this.u = player.getUniqueId();
    }

    @Override
    public void process(ByteArrayDataOutput out) {
        out.writeUTF(u.toString());
    }
}
