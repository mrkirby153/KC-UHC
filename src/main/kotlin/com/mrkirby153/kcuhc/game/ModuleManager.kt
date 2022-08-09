package com.mrkirby153.kcuhc.game

object ModuleManager {

    private val modules = mutableListOf<Module>()

    val loadedModules: List<Module>
        get() = modules.filter { it.loaded }

    /**
     * Registers the given [module] with the registry
     */
    fun register(module: Module) {
        if (modules.any { it.id == module.id }) {
            throw DuplicateModuleException(module.id)
        }
        modules.add(module)
    }

    fun getModules(): List<Module> = modules.toList()

    /**
     * Retrieves a module from the module registry by its [id]
     */
    fun getModuleById(id: String) = modules.firstOrNull { it.id == id }

    /**
     * Gets a module from the module registry
     */
    inline fun <reified T : Module> getModule(): T = getModules().first { it is T } as T

    /**
     * Executes [callback] if the given module [T] is loaded
     */
    inline fun <reified T : Module> whenLoaded(callback: (T) -> Unit) {
        val mod = getModule<T>()
        if (mod.loaded) {
            callback(mod)
        }
    }

    /**
     * Executes [callback] if the given module [T] is not loaded
     */
    inline fun <reified T : Module> whenUnloaded(callback: () -> Unit) {
        val mod = getModule<T>()
        if (!mod.loaded) {
            callback()
        }
    }
}

class DuplicateModuleException(id: String) : Exception("Duplicate module with the id $id")