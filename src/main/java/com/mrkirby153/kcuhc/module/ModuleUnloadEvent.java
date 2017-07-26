package com.mrkirby153.kcuhc.module;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a {@link UHCModule} is unloaded
 */
public class ModuleUnloadEvent extends Event implements Cancellable{
    private static HandlerList handlers = new HandlerList();

    private boolean canceled = false;
    private UHCModule module;

    public ModuleUnloadEvent(UHCModule module){
        this.module = module;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.canceled = cancel;
    }

    /**
     * Gets the module being unloaded
     * @return The module
     */
    public UHCModule getModule() {
        return module;
    }
}
