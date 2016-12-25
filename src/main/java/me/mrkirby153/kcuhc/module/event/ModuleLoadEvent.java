package me.mrkirby153.kcuhc.module.event;

import me.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event called when a module is about to load
 */
public class ModuleLoadEvent extends Event implements Cancellable {

    public static final HandlerList handlerList = new HandlerList();
    /**
     * The module that is being loaded
     */
    private final UHCModule module;
    private boolean canceled = false;

    public ModuleLoadEvent(UHCModule module) {
        this.module = module;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    /**
     * Gets the module that's being loaded
     *
     * @return The module
     */
    public UHCModule getModule() {
        return module;
    }

    @Override
    public boolean isCancelled() {
        return this.canceled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.canceled = b;
    }
}
