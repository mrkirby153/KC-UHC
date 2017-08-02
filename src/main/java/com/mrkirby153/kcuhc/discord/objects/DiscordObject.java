package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.DiscordRobot;

import java.util.function.Consumer;

public abstract class DiscordObject<T> {

    protected T object;
    protected DiscordRobot robot;

    public DiscordObject(DiscordRobot robot) {
        this.robot = robot;
    }

    public void create() {
        create(null);
    }

    public abstract void create(Consumer<T> callback);

    public abstract void delete();

    public T getObject() {
        return object;
    }
}
