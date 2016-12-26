package me.mrkirby153.kcuhc.module;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.module.event.ModuleUnloadEvent;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.ItemFactory;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class UHCModule implements Listener {

    private final String name;
    private final boolean autoLoad;
    private final Material material;
    private final int damage;
    protected boolean unregisterListener = true;
    private boolean listenerRegistered = false;
    private boolean loaded = false;
    private String description = "";

    private List<Class<? extends UHCModule>> depends = new ArrayList<>();

    public UHCModule(Material material, int damage, String name, boolean autoLoad, String description) {
        this.material = material;
        this.damage = damage;
        this.name = name;
        this.autoLoad = autoLoad;
        this.description = description;
    }

    public boolean autoLoad() {
        return autoLoad;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UHCModule && ((UHCModule) obj).name.equals(this.name);
    }

    public List<UHCModule> getDepends() {
        return depends.stream().map(ModuleRegistry::getModule).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    public void addDepends(Class<? extends UHCModule> depends) {
        this.depends.add(depends);
    }

    public String getDescription() {
        return description;
    }

    public ItemFactory getGuiIcon() {
        return new ItemFactory(this.material).data(this.damage).name(this.name);
    }

    public String getInternalName() {
        return this.name.toLowerCase().replaceAll("\\s", "-");
    }

    public String getName() {
        return name;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void onDisable() {

    }

    public abstract void onEnable();

    @EventHandler(ignoreCancelled = true)
    public void onModuleUnload(ModuleUnloadEvent event) {
        if (depends.contains(event.getModule().getClass())) {
            Bukkit.broadcastMessage(UtilChat.message("Unloading of " + ChatColor.GOLD + event.getModule().getName() + ChatColor.GRAY
                    + " prevented because " + ChatColor.GOLD + getName() + ChatColor.GRAY + " is still loaded!"));
            event.setCancelled(true);
        }
    }

    protected UHC getPlugin() {
        return UHC.getInstance();
    }

    public boolean load() {
        if (this.loaded) {
            throw new IllegalArgumentException("Attempted to load a module that was already loaded!");
        }
        getPlugin().getLogger().info("Loading module " + this.name);
        if (!listenerRegistered) {
            Bukkit.getServer().getPluginManager().registerEvents(this, getPlugin());
            this.listenerRegistered = true;
        }
        depends.forEach(ModuleRegistry::loadIfUnloaded);
        try {
            onEnable();
        } catch (Exception e) {
            e.printStackTrace();
            getPlugin().getLogger().info("An error occurred when loading the module " + this.name + ". It will remain unloaded");
            Bukkit.broadcastMessage(UtilChat.message("An error occurred when loading the module " + this.name));
            return false;
        }
        this.loaded = true;
        getPlugin().getLogger().info("Loaded " + this.name);
        return true;
    }

    public boolean unload() {
        if (!this.loaded) {
            throw new IllegalArgumentException("Attempted to unload a module that wasn't loaded!");
        }
        getPlugin().getLogger().info("Unloading module " + this.name);
        try {
            onDisable();
            if (unregisterListener) {
                HandlerList.unregisterAll(this);
                this.listenerRegistered = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            getPlugin().getLogger().info("An error occurred when unloading the module " + this.name + ". It will remain loaded");
            return false;
        }
        this.loaded = false;
        getPlugin().getLogger().info("Unloaded " + this.name);
        return true;
    }
}
