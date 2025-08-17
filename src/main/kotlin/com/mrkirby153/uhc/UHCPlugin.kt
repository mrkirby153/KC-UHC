package com.mrkirby153.uhc

import com.mrkirby153.uhc.game.UHCGame
import me.mrkirby153.kcutils.event.UpdateEventHandler
import org.bukkit.plugin.java.JavaPlugin

class UHCPlugin : JavaPlugin() {

    private lateinit var game: UHCGame
    private lateinit var updateEventHandler: UpdateEventHandler

    override fun onEnable() {
        saveDefaultConfig()

        updateEventHandler = UpdateEventHandler(this)
        updateEventHandler.load()

        game = UHCGame(this)
        game.init()

        val commands = Commands(this)
        commands.register()
    }

    override fun onDisable() {
        game.shutdown()
    }
}
