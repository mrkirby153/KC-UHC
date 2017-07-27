package com.mrkirby153.kcuhc.module;

import org.bukkit.Bukkit;
import org.reflections.Reflections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Registry for all {@link UHCModule}
 */
public class ModuleRegistry {

    public static ModuleRegistry INSTANCE;

    static {
        INSTANCE = new ModuleRegistry();
    }

    private HashSet<UHCModule> loadedModules = new HashSet<>();

    private HashSet<UHCModule> availableModules = new HashSet<>();

    /**
     * Gets all the modules available
     *
     * @return The module lost
     */
    public HashSet<UHCModule> availableModules() {
        return new HashSet<>(this.availableModules);
    }

    /**
     * Gets a list of all the loaded modules
     *
     * @return The module list
     */
    public Collection<UHCModule> getLoadedModules() {
        return this.loadedModules;
    }

    /**
     * Gets a {@link UHCModule} by its class
     *
     * @param clazz The module class
     * @return The module, or null if it wasn't available
     */
    @SuppressWarnings("unchecked")
    public <T extends UHCModule> T getModule(Class<T> clazz) {
        if (!UHCModule.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(clazz + " does not extend " + UHCModule.class);
        }
        for (UHCModule mod : availableModules) {
            if (mod.getClass().equals(clazz))
                return (T) mod;
        }
        return null;
    }

    /**
     * Gets a module by its internal name
     *
     * @param internalName The name of the module
     * @return The module, or null if it doesn't exist
     */
    public UHCModule getModuleByName(String internalName) {
        for (UHCModule m : availableModules) {
            if (m.getInternalName().equals(internalName))
                return m;
        }
        return null;
    }

    /**
     * Loads a {@link UHCModule}
     *
     * @param module The module to load
     */
    public void load(UHCModule module) {
        ModuleLoadEvent event = new ModuleLoadEvent(module);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        if (module.load()) {
            loadedModules.add(module);
        }
    }

    /**
     * Loads all modules
     */
    public void loadAll() {
        Reflections reflections = new Reflections("com.mrkirby153.kcuhc");
        Set<Class<? extends UHCModule>> modules = reflections.getSubTypesOf(UHCModule.class);

        modules.forEach(c -> {
            System.out.println("[MODULE] Attempting to register " + c.getName());
            try {
                UHCModule e = c.newInstance();
                availableModules.add(e);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        // Load all modules set to autoload
        availableModules.stream().filter(UHCModule::autoLoad).forEach(this::load);
    }

    /**
     * Checks if a module is loaded
     * @param clazz The module to check if loaded
     * @return True if the module is loaded
     */
    public boolean loaded(Class<? extends UHCModule> clazz) {
        for(UHCModule m : this.loadedModules){
            if(m.getClass().equals(clazz))
                return true;
        }
        return false;
    }

    /**
     * Unloads a {@link UHCModule}
     *
     * @param module The module to unload
     */
    public void unload(UHCModule module) {
        ModuleUnloadEvent event = new ModuleUnloadEvent(module);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        if (module.unload()) {
            loadedModules.remove(module);
        }
    }

}
