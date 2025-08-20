package com.mrkirby153.uhc.game

import com.mrkirby153.uhc.UHCPlugin
import com.mrkirby153.uhc.game.teams.TeamManager
import com.mrkirby153.uhc.listener.GlobalListener
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import kotlin.reflect.KClass

class UHCGame(val plugin: UHCPlugin) {

    val teamManager = TeamManager(this)

    var state: GameState = GameState.WAITING
        set(value) {
            if (field != value) {
                val event = GameStateChangeEvent(field, value)
                dispatchEvent(event)
                if (event.isCancelled) {
                    plugin.logger.warning { "Game state change from $field to $value was cancelled" }
                    return
                }
            }
            field = value
            initListener()
        }

    private var activeListener: Listener? = null

    fun init() {
        plugin.logger.info("Initializing UHC Game")
        teamManager.loadTeams()
        plugin.server.pluginManager.registerEvents(GlobalListener(this), plugin)
        initListener()
    }

    fun shutdown() {

    }


    fun dispatchEvent(event: Event) {
        plugin.server.pluginManager.callEvent(event)
    }


    private fun initListener() {
        val toInitialize = state.listener ?: return
        if (toInitialize::class == activeListener?.javaClass) {
            return
        }
        val instance = constructListener(toInitialize) ?: return
        activeListener?.apply {
            plugin.logger.info { "Unregistering listener: ${this::class}" }
            HandlerList.unregisterAll(this)
        }
        activeListener = instance
        plugin.server.pluginManager.registerEvents(instance, plugin)
        plugin.logger.info { "Registered listener for game state: $state (${activeListener!!::class})" }
    }


    private fun constructListener(clazz: KClass<out Listener>): Listener? {
        val constructor =
            clazz.constructors.firstOrNull { it.parameters.size == 1 && it.parameters.first().type.classifier == UHCGame::class }
        if (constructor == null) {
            plugin.logger.warning { "Class $clazz has no valid constructor (Missing a constructor with one argument: UHCGame)" }
            return null
        }
        return constructor.call(this)
    }
}