package com.mrkirby153.uhc

import co.aikar.commands.PaperCommandManager
import com.mrkirby153.uhc.game.UHCGame
import me.mrkirby153.kcutils.event.UpdateEventHandler
import org.bukkit.plugin.java.JavaPlugin

class UHCPlugin : JavaPlugin() {

    lateinit var game: UHCGame
    private lateinit var commandManager: PaperCommandManager
    private lateinit var updateEventHandler: UpdateEventHandler

    override fun onEnable() {
        saveDefaultConfig()

        updateEventHandler = UpdateEventHandler(this)
        updateEventHandler.load()

        game = UHCGame(this)
        game.init()

        commandManager = PaperCommandManager(this)
        val commands = Commands(this, commandManager)
        commands.register()
    }

    override fun onDisable() {
        game.shutdown()
    }
}
