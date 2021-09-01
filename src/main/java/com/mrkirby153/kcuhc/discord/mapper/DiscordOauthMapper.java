package com.mrkirby153.kcuhc.discord.mapper;

import net.dv8tion.jda.api.entities.User;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordOauthMapper implements PlayerMapper{

    @Override
    public User getUser(UUID uuid) {
        return null;
    }

    @Override
    public void createLink(Player player) {

    }

    @Override
    public void forceLink(Player player, String id) {

    }

    @Override
    public String getCode(UUID uuid) {
        return null;
    }
}
