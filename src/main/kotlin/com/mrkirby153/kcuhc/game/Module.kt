package com.mrkirby153.kcuhc.game

import com.mrkirby153.kcuhc.utils.getPlugin
import com.mrkirby153.kcuhc.utils.register
import com.mrkirby153.kcuhc.utils.unregister
import org.bukkit.Material
import org.bukkit.event.Listener

/**
 * Abstract module for game elements
 */
open class Module(
    val name: String,
    val description: String,
    val item: Material,
    val category: ModuleCategory
) : Listener {

    var loaded = false
        private set

    var autoLoad = false
        protected set

    /**
     * The module's unique identifier
     */
    val id: String
        get() = name.lowercase().replace(' ', '-')

    /**
     * Loads the module
     */
    fun load() {
        if (loaded) {
            throw IllegalStateException("Module $id is already loaded")
        }
        register()
        try {
            onLoad()
        } catch (e: Exception) {
            e.printStackTrace()
            getPlugin().logger.severe("An error occurred loading the module $id. It will remain unloaded")
        }
        loaded = true
    }

    /**
     * Unloads the module
     */
    fun unload() {
        if (!loaded) {
            throw IllegalStateException("Module $id is not loaded")
        }
        unregister()
        try {
            onUnload()
        } catch (e: Exception) {
            e.printStackTrace()
            getPlugin().logger.severe("An error occurred unloading the module $id. It will remain loaded")
        }
        loaded = false
    }

    /**
     * Reloads the module
     */
    fun reload() {
        if (!loaded) {
            getPlugin().logger.warning("Attempting to reload $id, but it is not loaded. Loading instead...")
            load()
        }
        try {
            onReload()
        } catch (e: Exception) {
            e.printStackTrace()
            getPlugin().logger.severe("Unhandled exception when reloading $id")
        }
    }

    /**
     * Called when the module is loaded
     */
    open fun onLoad() {

    }

    /**
     * Called when the module is unloaded
     */
    open fun onUnload() {

    }

    /**
     * Called when the module is reloaded
     */
    open fun onReload() {

    }
}

/**
 * Enum class for module categories
 */
enum class ModuleCategory(val icon: Material) {
    UNKNOWN(Material.PAPER)
}