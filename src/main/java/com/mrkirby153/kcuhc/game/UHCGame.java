package com.mrkirby153.kcuhc.game;

import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import org.bukkit.Bukkit;

/**
 * The main game class
 */
public class UHCGame {

    /**
     * The current state of the game
     */
    private GameState currentState = GameState.WAITING;

    /**
     * Gets the current state of the game
     * @return The current state of the game
     */
    public GameState getCurrentState() {
        return currentState;
    }

    /**
     * Sets the current state of the game, firing {@link com.mrkirby153.kcuhc.game.event.GameStateChangeEvent}
     * @param newState The new state of the game
     */
    public void setCurrentState(GameState newState) {
        this.currentState = newState;
        Bukkit.getServer().getPluginManager().callEvent(new GameStateChangeEvent(newState));
    }
}
