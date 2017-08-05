package com.mrkirby153.kcuhc.module;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mrkirby153.kcuhc.UHC;
import org.bukkit.Bukkit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.reflections.Reflections;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for all {@link UHCModule}
 */
public class ModuleRegistry {

    public static ModuleRegistry INSTANCE;

    private static File presetDirectory;

    static {
        INSTANCE = new ModuleRegistry();
    }

    private HashSet<UHCModule> loadedModules = new HashSet<>();
    private HashSet<UHCModule> availableModules = new HashSet<>();

    /**
     * Sets the folder where presets are saved
     *
     * @param directory The directory
     */
    public static void setPresetDirectory(File directory) {
        if (!directory.exists())
            directory.mkdirs();
        presetDirectory = directory;
    }

    /**
     * Gets all the modules available
     *
     * @return The module lost
     */
    public HashSet<UHCModule> availableModules() {
        return new HashSet<>(this.availableModules);
    }

    /**
     * Gets a list of the available presets
     *
     * @return A list of available presets
     */
    public List<String> getAvailablePresets() {
        ArrayList<String> list = new ArrayList<>();
        if (presetDirectory != null && presetDirectory.listFiles() != null)
            Arrays.stream(presetDirectory.listFiles()).map(File::getName).forEach(f -> {
                list.add(f.replace(".json", ""));
            });
        return list;
    }

    /**
     * Gets a {@link UHCModule} if its loaded.
     *
     * @param clazz The module to get
     * @return An {@link Optional} of the module.
     */
    public <T extends UHCModule> Optional<T> getLoadedModule(Class<T> clazz) {
        T m = getModule(clazz);
        if (m.isLoaded()) {
            return Optional.of(m);
        } else {
            return Optional.empty();
        }
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

        Injector injector = Guice.createInjector(new GuiceUHCModule(UHC.getPlugin(UHC.class)));

        modules.forEach(c -> {
            System.out.println("[MODULE] Attempting to register " + c.getName());
            UHCModule module = injector.getInstance(c);
            availableModules.add(module);
           /* try {
                // Find a constructor with a JavaPlugin
                UHCModule m;
                try {
                    Constructor constructor = c.getConstructor(UHC.class);
                    m = (UHCModule) constructor.newInstance(UHC.getPlugin(UHC.class));
                } catch (NoSuchMethodException e) {
                    m = c.newInstance();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    return;
                }
                availableModules.add(m);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }*/
        });
        // Load all modules set to autoload
        availableModules.stream().filter(UHCModule::autoLoad).filter(m -> !loaded(m.getClass())).forEach(this::load);
    }

    /**
     * Loads modules from a preset
     *
     * @param presetName The preset to load
     * @throws java.io.FileNotFoundException If the preset doesn't exist
     * @throws IOException                   If there was an error loading the preset
     */
    public void loadFromPreset(String presetName) throws IOException {
        FileInputStream inputStream = new FileInputStream(new File(presetDirectory, presetName + ".json"));
        JSONObject object = new JSONObject(new JSONTokener(inputStream));
        inputStream.close();

        JSONArray array = object.getJSONArray("loaded-modules");
        // Unload all modules
        new HashSet<>(this.loadedModules).forEach(this::forceUnload);
        array.forEach(o -> {
            UHCModule module = getModuleByName(o.toString());
            if (module != null && !module.isLoaded()) {
                forceLoad(module);
            }
        });

        JSONObject data = object.getJSONObject("settings");
        HashMap<String, String> dataMap = new HashMap<>();
        data.keySet().forEach(key -> dataMap.put(key, data.getString(key)));
        loadedModules.forEach(m -> m.loadData(dataMap));
    }

    /**
     * Checks if a module is loaded
     *
     * @param clazz The module to check if loaded
     * @return True if the module is loaded
     */
    public boolean loaded(Class<? extends UHCModule> clazz) {
        for (UHCModule m : this.loadedModules) {
            if (m.getClass().equals(clazz))
                return true;
        }
        return false;
    }

    /**
     * Saves the module to a preset
     *
     * @param presetName The name to save
     * @throws IOException If any error occurrs
     */
    public void saveToPreset(String presetName) throws IOException {
        JSONObject object = new JSONObject();
        loadedModules.forEach(m -> object.append("loaded-modules", m.getInternalName()));
        // Save module data
        HashMap<String, String> data = new HashMap<>();
        loadedModules.forEach(m -> m.saveData(data));

        JSONObject dataObj = new JSONObject();
        data.forEach(dataObj::put);
        object.put("settings", dataObj);

        FileWriter writer = new FileWriter(new File(presetDirectory, presetName + ".json"));
        writer.write(object.toString(3));
        writer.close();
    }

    /**
     * Shuts down the module system, force unloading all the loaded modules
     */
    public void shutdown() {
        this.loadedModules.forEach(this::forceLoad);
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

    /**
     * Force loads a module, bypassing the event
     *
     * @param mod The module to load
     */
    private void forceLoad(UHCModule mod) {
        if (mod != null) {
            if (mod.isLoaded())
                return;
            if (mod.load())
                loadedModules.add(mod);
        }
    }

    /**
     * Force unload a module, bypassing the event
     *
     * @param module The module to unload
     */
    private void forceUnload(UHCModule module) {
        if (module.unload())
            loadedModules.remove(module);
    }
}
