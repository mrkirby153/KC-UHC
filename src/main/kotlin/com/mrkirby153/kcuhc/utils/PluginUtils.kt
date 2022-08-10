package com.mrkirby153.kcuhc.utils

import com.mrkirby153.kcuhc.UHCPlugin
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

private var pluginInstance: UHCPlugin? = null

fun getPlugin(): UHCPlugin {
    if (pluginInstance == null) {
        throw IllegalStateException("Plugin has not been initialized yet!")
    }
    return pluginInstance!!
}

fun setPlugin(instance: UHCPlugin) {
    pluginInstance = instance
}

private fun fireEvent(event: Event) {
    getPlugin().server.pluginManager.callEvent(event)
}

/**
 * Fires an event running the [callback] if the event succeeds
 */
fun Cancellable.fire(callback: (Event.() -> Unit)? = null) {
    val event = this as? Event ?: throw IllegalStateException("Attempting to fire a non-event")
    fireEvent(event)
    if (!isCancelled) {
        callback?.invoke(event)
    }
}

/**
 * Registers the listener
 */
fun Listener.register() {
    getPlugin().server.pluginManager.registerEvents(this, getPlugin())
}

/**
 * Unregisters the listener
 */
fun Listener.unregister() {
    HandlerList.unregisterAll(this)
}