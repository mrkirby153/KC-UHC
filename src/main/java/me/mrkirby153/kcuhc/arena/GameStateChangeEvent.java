package me.mrkirby153.kcuhc.arena;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameStateChangeEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final UHCArena.State from;
    private final UHCArena.State to;

    public GameStateChangeEvent(UHCArena.State from, UHCArena.State to) {
        this.from = from;
        this.to = to;
    }

    public UHCArena.State getFrom() {
        return from;
    }

    public UHCArena.State getTo() {
        return to;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
