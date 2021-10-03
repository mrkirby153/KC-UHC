package com.mrkirby153.kcuhc.game;

import org.bukkit.Sound;

/**
 * An event scheduled to be run at a later date
 */
public interface ScheduledEvent {

    /**
     * Gets the name of the event
     *
     * @return The name of the event
     */
    String getName();

    /**
     * The action(s) to perform when the event is fired
     */
    void run();

    /**
     * Any actions that should be run when the event is canceled
     */
    default void onCancel() {

    }

    /**
     * If the event should be announced
     *
     * @param msLeft The amount of ms left until start
     *
     * @return True if the event should be broadcasted
     */
    default boolean shouldAnnounce(long msLeft) {
        double secondsLeft = msLeft / 1000D;
        return secondsLeft <= 10;
    }

    /**
     * The sound accompanied by announcements
     *
     * @return The sound
     */
    default Sound notifySound() {
        return Sound.UI_BUTTON_CLICK;
    }

    /**
     * The sound played when the event starts
     *
     * @return The sound
     */
    default Sound startSound() {
        return Sound.BLOCK_NOTE_BLOCK_PLING;
    }
}
