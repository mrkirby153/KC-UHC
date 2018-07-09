package com.mrkirby153.kcuhc.discord.mapper;

import net.dv8tion.jda.core.entities.User;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface PlayerMapper {

    /**
     * Gets the discord {@link User} associated with the provided {@link UUID}
     *
     * @param uuid The UUID of the player
     *
     * @return The user, or null
     */
    User getUser(UUID uuid);

    /**
     * Starts the link creation process for associating a Minecraft account to a discord account
     *
     * @param player The player to begin the process for
     */
    void createLink(Player player);

    /**
     * Forcibly creates a link between the two accounts
     *
     * @param player The player
     * @param id The id of the account to link
     */
    void forceLink(Player player, String id);
}
