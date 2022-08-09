package com.mrkirby153.kcuhc.game.phase

import org.bukkit.event.Listener

/**
 * A game phase
 */
interface GamePhase : Listener {

    /**
     * Called when this phase is activated
     */
    fun activate()

    /**
     * Called when this phase is deactivated
     */
    fun deactivate()

    /**
     * Returns true when this phase should be active
     */
    fun shouldActivate(): Boolean
}