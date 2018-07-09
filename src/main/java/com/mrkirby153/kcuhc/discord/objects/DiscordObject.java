package com.mrkirby153.kcuhc.discord.objects;

import com.mrkirby153.kcuhc.discord.Deletable;

import java.util.Optional;
import java.util.function.Consumer;

public interface DiscordObject<T> extends Deletable {

    /**
     * Creates the object
     *
     * @param consumer A callback to run after the channel has been created
     */
    void create(Consumer<T> consumer);

    default void create() {
        this.create(null);
    }

    /**
     * Gets the object
     *
     * @return An optional of the object
     */
    Optional<T> get();
}
