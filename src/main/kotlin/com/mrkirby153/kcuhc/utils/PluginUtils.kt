package com.mrkirby153.kcuhc.utils

import com.mrkirby153.kcuhc.UHCPlugin
import org.bukkit.event.Cancellable
import org.bukkit.event.Event

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
 * Fires an event running the [callback] if the event succeeds. If this event does not implement
 * [Cancellable] then the callback will always fire.
 */
fun Event.fire(callback: (Event.() -> Unit)? = null) {
    fireEvent(this)
    if (this is Cancellable) {
        if (!isCancelled) {
            callback?.invoke(this)
        }
    } else {
        callback?.invoke(this)
    }
}