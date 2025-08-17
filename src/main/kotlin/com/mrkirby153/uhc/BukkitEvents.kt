package com.mrkirby153.uhc

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

open class BukkitEvent : Event() {
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    companion object {
        val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }
}

open class CancellableBukkitEvent : BukkitEvent(), Cancellable {

    private var canceled = false
    override fun isCancelled() = canceled

    override fun setCancelled(cancel: Boolean) {
        this.canceled = cancel
    }
}