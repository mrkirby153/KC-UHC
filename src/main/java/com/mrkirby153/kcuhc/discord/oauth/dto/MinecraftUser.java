package com.mrkirby153.kcuhc.discord.oauth.dto;

public class MinecraftUser {

    private final String uuid;
    private final String username;

    public MinecraftUser(String uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public String getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }
}
