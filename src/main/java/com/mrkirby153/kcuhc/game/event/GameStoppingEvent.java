package com.mrkirby153.kcuhc.game.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when the game stops
 */
public class GameStoppingEvent extends Event implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();

    private boolean canceled;

    private Reason reason;

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public GameStoppingEvent(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
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


    public enum Reason {
        ABORTED,
        NORMAL
    }
}
