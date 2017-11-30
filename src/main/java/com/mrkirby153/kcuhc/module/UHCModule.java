package com.mrkirby153.kcuhc.module;

import com.mrkirby153.kcuhc.UHC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.HashMap;

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
     * Gets the item's damage
     *
     * @return The damage
     */
    public int getDamage() {
        return damage;
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
     * Gets the material for the gui
     *
     * @return The material
     */
    public Material getGuiItem() {
        return guiItem;
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
     * Gets the module's name
     *
     * @return The name of the module
     */
    public String getModuleName() {
        return moduleName;
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
     * Gets if the module is loaded
     *
     * @return True if the module is loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Loads the module
     */
    public boolean load() {
        if (this.loaded) {
            throw new IllegalArgumentException(
                "Attempted to load a module that was already loaded!");
        }
        Bukkit.getServer().getPluginManager().registerEvents(this, UHC.getPlugin(UHC.class));
        try {
            onLoad();
        } catch (Exception e) {
            UHC.getPlugin(UHC.class).getLogger().severe(
                "An error occurred when loading the module " + this.moduleName
                    + ". It will remain unloaded");
            return false;
        }
        this.loaded = true;
        return true;
    }

    public void loadData(HashMap<String, String> data) {

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

    public void saveData(HashMap<String, String> data) {

    }

    /**
     * Unloads the module
     */
    public boolean unload() {
        try {
            onUnload();
            HandlerList.unregisterAll(this);
        } catch (Exception e) {
            UHC.getPlugin(UHC.class).getLogger().severe(
                "An error occurred when unloading the module " + this.moduleName
                    + ". It will remain loaded");
            return false;
        }
        this.loaded = false;
        return true;
    }
}
