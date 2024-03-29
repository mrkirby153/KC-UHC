package com.mrkirby153.kcuhc.module;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.settings.ModuleSetting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract module for various game elements
 */
public abstract class UHCModule implements Listener {

    private final String moduleName;
    private final Material guiItem;
    private final String description;

    protected boolean autoLoad = false;
    private boolean loaded = false;

    public UHCModule(String moduleName, String description, Material guiItem) {
        this.moduleName = moduleName;
        this.guiItem = guiItem;
        this.description = description;
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
    public final boolean load() {
        if (this.loaded) {
            throw new IllegalArgumentException(
                "Attempted to load a module that was already loaded!");
        }
        Bukkit.getServer().getPluginManager().registerEvents(this, UHC.getPlugin(UHC.class));
        try {
            onLoad();
        } catch (Exception e) {
            e.printStackTrace();
            UHC.getPlugin(UHC.class).getLogger().severe(
                "An error occurred when loading the module " + this.moduleName
                    + ". It will remain unloaded");
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
     * Called when the module is reloaded
     */
    public void onReload() {

    }

    /**
     * Called when a setting is changed
     *
     * @param setting The setting that was changed
     */
    public void onSettingChange(ModuleSetting<?> setting) {

    }

    /**
     * Unloads the module
     */
    public final boolean unload() {
        try {
            onUnload();
            HandlerList.unregisterAll(this);
        } catch (Exception e) {
            e.printStackTrace();
            UHC.getPlugin(UHC.class).getLogger().severe(
                "An error occurred when unloading the module " + this.moduleName
                    + ". It will remain loaded");
            return false;
        }
        this.loaded = false;
        return true;
    }

    /**
     * Reloads the module
     */
    public final void reload(boolean full) {
        if (full) {
            unload();
            load();
        } else {
            onReload();
        }
    }

    /**
     * Gets a list of settings available
     *
     * @return The list of settings
     */
    public final Map<String, ModuleSetting> getSettings() {
        Map<String, ModuleSetting> map = new HashMap<>();
        for (Field f : this.getClass().getDeclaredFields()) {
            if (ModuleSetting.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                try {
                    map.put(f.getName(), (ModuleSetting) f.get(this));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }
}
