package com.mrkirby153.kcuhc

import com.mrkirby153.kcuhc.game.Game
import com.mrkirby153.kcuhc.game.phase.GamePhaseManager
import com.mrkirby153.kcuhc.utils.setPlugin
import me.mrkirby153.kcutils.event.UpdateEventHandler
import org.bukkit.plugin.java.JavaPlugin

class UHCPlugin : JavaPlugin() {

    lateinit var updateEventHandler: UpdateEventHandler

    lateinit var game: Game

    override fun onEnable() {
        setPlugin(this)
        saveDefaultConfig()

        // Initialize TickEvent
        updateEventHandler = UpdateEventHandler(this)
        updateEventHandler.load()

        server.pluginManager.registerEvents(GamePhaseManager, this)

        game = Game(this)
        game.initialize()
    }

    override fun onDisable() {

    }
}