package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.IDeletable;

import java.util.function.Consumer;

/**
 * Abstract class representing a discord object
 */
public abstract class DiscordObject<T> implements IDeletable {

    private T object;

    /**
     * Creates the object
     *
     * @param callback A callback to run when the creation is complete
     */
    public abstract void create(Consumer<T> callback);

    /**
     * Creates the object
     */
    public void create() {
        this.create(null);
    }

    /**
     * Returns the object
     *
     * @return The object or null if it hasn't been created
     */
    public T get() {
        return object;
    }

    /**
     * Sets the object once it's been created
     *
     * @param object The object
     */
    protected void set(T object) {
        this.object = object;
    }
}
