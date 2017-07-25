package com.mrkirby153.kcuhc.game.event;

import com.mrkirby153.kcuhc.game.GameState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when the game's state changes
 */
public class GameStateChangeEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final GameState to;

    public GameStateChangeEvent(GameState to) {
        this.to = to;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    /**
     * Gets the {@link GameState} the game is transitioning to
     *
     * @return The state
     */
    public GameState getTo() {
        return to;
    }
}
