package com.mrkirby153.kcuhc.game.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when the game is starting
 */
public class GameStartingEvent extends Event implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();

    private boolean canceled = false;

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public boolean isCancelled() {
        return this.canceled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.canceled = b;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
