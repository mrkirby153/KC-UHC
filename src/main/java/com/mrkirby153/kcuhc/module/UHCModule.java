package com.mrkirby153.kcuhc.module;

import com.mrkirby153.kcuhc.UHC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/**
 * Abstract module for various game elements
 */
public abstract class UHCModule implements Listener {

    private final String moduleName;
    private final Material guiItem;
    private final int damage;
    private final String description;

    protected boolean autoLoad = false;
    private boolean loaded = false;

    public UHCModule(String moduleName, String description, Material guiItem, int damage) {
        this.moduleName = moduleName;
        this.guiItem = guiItem;
        this.damage = damage;
        this.description = description;
    }

    public UHCModule(String moduleName, String description, Material guiItem) {
        this(moduleName, description, guiItem, 0);
    }

    /**
     * Gets if the plugin should load on startup
     *
     * @return True if the plugin should load on startup
     */
    public boolean autoLoad() {
        return autoLoad;
    }

    /**
     * Gets the module's description
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the internal name of the module
     *
     * @return The module's internal name
     */
    public String getInternalName() {
        return this.moduleName.toLowerCase().replace(' ', '-');
    }

    /**
     * Gets the module's friendly name
     *
     * @return The name of the module
     */
    public String getName() {
        return moduleName;
    }

    /**
     * Loads the module
     */
    public boolean load() {
        if (this.loaded) {
            throw new IllegalArgumentException("Attempted to load a module that was already loaded!");
        }
        Bukkit.getServer().getPluginManager().registerEvents(this, getPlugin());
        try {
            onLoad();
        } catch (Exception e) {
            getPlugin().getLogger().severe("An error occurred when loading the module " + this.moduleName + ". It will remain unloaded");
            return false;
        }
        this.loaded = true;
        return true;
    }

    /**
     * Called when the module is loaded
     */
    public void onLoad() {

    }

    /**
     * Called when the module is unloaded
     */
    public void onUnload() {

    }

    /**
     * Unloads the module
     */
    public boolean unload() {
        try {
            onUnload();
            HandlerList.unregisterAll(this);
        } catch (Exception e) {
            getPlugin().getLogger().severe("An error occurred when unloading the module " + this.moduleName + ". It will remain loaded");
            return false;
        }
        this.loaded = false;
        return true;
    }

    /**
     * Gets the main plugin instance
     *
     * @return The plugin instance
     */
    protected UHC getPlugin() {
        return UHC.getPlugin(UHC.class);
    }
}
