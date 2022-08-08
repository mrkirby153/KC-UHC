package com.mrkirby153.kcuhc.game

import com.mrkirby153.kcuhc.UHCPlugin
import com.mrkirby153.kcuhc.events.GameInitializingEvent
import com.mrkirby153.kcuhc.events.GameStateChangeEvent
import com.mrkirby153.kcuhc.utils.fire
import org.bukkit.GameRule
import org.bukkit.World
import kotlin.system.measureTimeMillis

/**
 * The main UHC game
 */
class Game(private val plugin: UHCPlugin) {

    var state: GameState = GameState.UNINITIALIZED
        set(value) {
            plugin.logger.info("[GAME STATE] Changing from $field -> $value")
            GameStateChangeEvent(field, value).fire()
            field = value
        }


    val uhcWorld: World = plugin.server.worlds[0]

    /**
     * Sets up the game world
     */
    fun initialize() {
        state = GameState.INITIALIZING
        GameInitializingEvent().fire()
        plugin.logger.info("Initializing game...")
        val time = measureTimeMillis {
            initializeWorld()
        }
        plugin.logger.info("Game initialized in $time ms")
        state = GameState.INITIALIZING
    }

    private fun initializeWorld() {
        plugin.logger.info("Initializing main UHC world: ${uhcWorld.name}")
        uhcWorld.time = 1200
        uhcWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        uhcWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        uhcWorld.setStorm(false)
        uhcWorld.isThundering = false
    }
}