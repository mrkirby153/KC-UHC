package com.mrkirby153.kcuhc

import com.mrkirby153.kcuhc.events.GameInitializingEvent
import me.mrkirby153.kcutils.event.UpdateEventHandler
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class UHCPlugin : JavaPlugin(), Listener {

    lateinit var updateEventHandler: UpdateEventHandler

    override fun onEnable() {
        saveDefaultConfig()

        // Initialize TickEvent
        updateEventHandler = UpdateEventHandler(this)
        updateEventHandler.load()
        server.pluginManager.callEvent(GameInitializingEvent())
    }

    override fun onDisable() {

    }
}