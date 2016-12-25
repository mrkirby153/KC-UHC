package me.mrkirby153.kcuhc.module.event;

import me.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ModuleUnloadEvent extends Event implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();

    private boolean canceled = false;
    /**
     * The module being unloaded
     */
    private final UHCModule module;

    public ModuleUnloadEvent(UHCModule module) {
        this.module = module;
    }

    /**
     * Gets the module that's being unloaded
     * @return The module being unloaded
     */
    public UHCModule getModule() {
        return module;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public HandlerList getHandlers() {
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
}
