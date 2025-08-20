package com.mrkirby153.uhc.game

import com.mrkirby153.uhc.listener.GameListener
import com.mrkirby153.uhc.listener.IdleListener
import org.bukkit.event.Listener
import kotlin.reflect.KClass

/**
 * The current state of the game
 */
enum class GameState(val listener: KClass<out Listener>? = null) {
    /**
     * The game is waiting to start
     */
    WAITING(IdleListener::class),

    /**
     * The game is starting
     */
    COUNTDOWN,

    /**
     * The game is running
     */
    RUNNING(GameListener::class),

    /**
     * The game has ended
     */
    ENDED(IdleListener::class),
}