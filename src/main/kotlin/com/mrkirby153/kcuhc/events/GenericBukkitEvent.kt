package com.mrkirby153.kcuhc.events

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * A generic bukkit event. Implements event handlers
 */
open class GenericBukkitEvent : Event() {
    override fun getHandlers() = Companion.handlers

    companion object {
        val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }
}

/**
 * A generic bukkit event that can be canceled
 */
open class GenericCancellableEvent : GenericBukkitEvent(), Cancellable {

    private var canceled = false
    override fun isCancelled() = canceled

    override fun setCancelled(cancel: Boolean) {
        canceled = cancel
    }
}