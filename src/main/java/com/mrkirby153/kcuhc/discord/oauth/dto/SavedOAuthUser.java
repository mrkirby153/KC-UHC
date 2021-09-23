package com.mrkirby153.kcuhc.discord.oauth.dto;

public class SavedOAuthUser {

    private final String username;
    private final String discrim;
    private final String id;

    public SavedOAuthUser(String username, String discrim, String id) {
        this.username = username;
        this.discrim = discrim;
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getDiscrim() {
        return discrim;
    }

    public String getId() {
        return id;
    }
}
