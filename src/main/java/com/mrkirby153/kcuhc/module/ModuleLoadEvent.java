package com.mrkirby153.kcuhc.module;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a {@link UHCModule} is loaded
 */
public class ModuleLoadEvent extends Event implements Cancellable {
    private static HandlerList handlers = new HandlerList();

    private boolean canceled = false;
    private UHCModule module;

    public ModuleLoadEvent(UHCModule module) {
        this.module = module;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Gets the module being loaded
     *
     * @return The module
     */
    public UHCModule getModule() {
        return module;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.canceled = cancel;
    }
}
