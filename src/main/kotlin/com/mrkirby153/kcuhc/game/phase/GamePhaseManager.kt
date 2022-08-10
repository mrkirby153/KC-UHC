package com.mrkirby153.kcuhc.game.phase

import com.mrkirby153.kcuhc.game.GameState
import com.mrkirby153.kcuhc.utils.getPlugin
import com.mrkirby153.kcuhc.utils.register
import com.mrkirby153.kcuhc.utils.unregister
import me.mrkirby153.kcutils.event.UpdateEvent
import me.mrkirby153.kcutils.event.UpdateType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object GamePhaseManager : Listener {

    private val phases = mutableListOf<PhaseMetadata>()

    /**
     * Registers a game phase with the phase manager
     */
    fun register(name: String, phase: GamePhase) {
        phases.add(PhaseMetadata(phase, name))
    }

    /**
     * Unregisters a game [phase] from the phase manager
     */
    fun unregister(phase: GamePhase) {
        val toRemove = phases.filter { it.phase == phase }
        toRemove.forEach {
            if (it.active) {
                getPlugin().logger.info("Deactivating phase ${it.name}, it has been unregistered")
                deactivate(it)
            }
        }
    }

    /**
     * Unregisters a game phase with the given [name] from the phase manager
     */
    fun unregister(name: String) {
        val toRemove = phases.filter { it.name == name }
        toRemove.forEach {
            if (it.active) {
                getPlugin().logger.info("Deactivating phase ${it.name}, it has been unregistered")
                deactivate(it)
            }
        }
        phases.removeAll(toRemove)
    }

    private fun updatePhases() {
        val toActivate = phases.filter { !it.active }.filter { it.phase.shouldActivate() }
        val toDeactivate = phases.filter { it.active }.filter { !it.phase.shouldActivate() }
        if (toActivate.isNotEmpty()) {
            getPlugin().logger.info(
                "Activating phases ${
                    toActivate.joinToString(", ") { it.name }
                }"
            )
            toActivate.forEach { activate(it) }
        }
        if (toDeactivate.isNotEmpty()) {
            getPlugin().logger.info("Deactivating phases ${toDeactivate.joinToString(", ") { it.name }}")
            toDeactivate.forEach { deactivate(it) }
        }
    }

    private fun activate(metadata: PhaseMetadata) {
        metadata.active = true
        metadata.phase.activate()
        metadata.phase.register()
    }

    private fun deactivate(metadata: PhaseMetadata) {
        metadata.active = false
        metadata.phase.deactivate()
        metadata.phase.unregister()
    }

    @EventHandler
    fun tickEvent(event: UpdateEvent) {
        if (event.type != UpdateType.FAST) {
            return
        }
        if (getPlugin().game.state != GameState.RUNNING && phases.any { it.active }) {
            getPlugin().logger.info("Deactivating all phases, game is not running")
            phases.filter { it.active }.forEach { deactivate(it) }
            return
        }
        updatePhases()
    }
}

private data class PhaseMetadata(
    val phase: GamePhase,
    val name: String,
    var active: Boolean = false
)