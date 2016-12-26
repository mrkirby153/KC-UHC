package me.mrkirby153.kcuhc.module;

import me.mrkirby153.kcuhc.module.event.ModuleLoadEvent;
import me.mrkirby153.kcuhc.module.event.ModuleUnloadEvent;
import org.bukkit.Bukkit;

import java.util.*;

/**
 * Handles loading and unloading of modules
 */
public class ModuleRegistry {

    private static HashSet<UHCModule> availableModules = new HashSet<>();
    private static HashSet<UHCModule> loadedModules = new HashSet<>();

    /**
     * Returns a lsit of modules available to the Module registry
     *
     * @return The modules
     */
    public static Collection<UHCModule> allModules() {
        List<UHCModule> list = new ArrayList<>(availableModules);
        list.sort(((o1, o2) -> {
            if (o1.isLoaded()) {
                if (o2.isLoaded())
                    return o1.getName().compareTo(o2.getName());
                return -1;
            } else {
                if (!o2.isLoaded())
                    return o1.getName().compareTo(o2.getName());
                return 1;
            }
        }));
        return list;
    }

    /**
     * Gets a loaded module by its class
     *
     * @param clazz The class of the module
     * @param <T>   A class extending {@link UHCModule}
     * @return The module
     */
    public static <T extends UHCModule> Optional<T> getLoadedModule(Class<T> clazz) {
        if (clazz == null)
            throw new IllegalArgumentException("Null cannot have a module!");
        for (UHCModule m : loadedModules) {
            if (m.getClass().equals(clazz)) {
                return Optional.of(clazz.cast(m));
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the module (which may or may not be loaded) by its class
     *
     * @param clazz The class of the module
     * @param <T>   A class extending {@link UHCModule}
     * @return The module
     */
    public static <T extends UHCModule> Optional<T> getModule(Class<T> clazz) {
        if (clazz == null)
            throw new IllegalArgumentException("Null cannot have a module!");
        for (UHCModule m : availableModules) {
            if (m.getClass().equals(clazz)) {
                return Optional.of(clazz.cast(m));
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if the module specified by the class is loaded
     *
     * @param clazz The class of the module
     * @param <T>   A class extending {@link UHCModule}
     * @return If the module is loaded
     */
    public static <T extends UHCModule> boolean isLoaded(Class<T> clazz) {
        return getLoadedModule(clazz).isPresent();
    }

    /**
     * Loads a module if it isn't loaded
     *
     * @param clazz The module to  load
     * @param <T>   A class extending {@link UHCModule}
     */
    public static <T extends UHCModule> void loadIfUnloaded(Class<T> clazz) {
        if (isLoaded(clazz))
            return;
        getModule(clazz).ifPresent(ModuleRegistry::loadModule);
    }

    /**
     * Loads a module by its instance
     *
     * @param module The module to load
     */
    public static void loadModule(UHCModule module) {
        if (loadedModules.contains(module)) {
            throw new IllegalArgumentException("Attempted to load an already loaded module!");
        }
        ModuleLoadEvent event = new ModuleLoadEvent(module);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        if (module.load())
            loadedModules.add(module);
    }

    /**
     * Load all the modules which are registered to be loaded on start
     */
    public static void loadModulesOnStart() {
        availableModules.stream().filter(UHCModule::autoLoad).forEach(mod -> {
            loadIfUnloaded(mod.getClass());
        });
    }

    /**
     * Returns a list of modules that are loaded
     *
     * @return The modules that are loaded
     */
    public static Collection<UHCModule> loadedModules() {
        List<UHCModule> list = new ArrayList<>(loadedModules);
        list.sort(Comparator.comparing(UHCModule::getName));
        return list;
    }

    /**
     * Registers a module to be loaded by the registry
     *
     * @param module The module to register
     */
    public static void registerModule(UHCModule module) {
        if (availableModules.contains(module)) {
            throw new IllegalArgumentException("Attempted to register a module twice!");
        }
        availableModules.add(module);
    }

    public static void unloadAll() {

    }

    /**
     * Unloads a module
     *
     * @param module The module to unload
     */
    public static void unloadModule(UHCModule module) {
        if (!loadedModules.contains(module)) {
            throw new IllegalArgumentException("Attempted to unload a module that isn't loaded!");
        }
        ModuleUnloadEvent event = new ModuleUnloadEvent(module);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        if (module.unload())
            loadedModules.remove(module);
    }
}
