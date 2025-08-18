package com.mrkirby153.uhc

import com.mrkirby153.uhc.game.teams.UHCTeam
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

/**
 * A data class representing a player in the UHC game
 */
class UHCPlayer(
    private val plugin: UHCPlugin,
    private val playerUUID: UUID
) {

    val player: Player
        get() = plugin.server.getPlayer(playerUUID)
            ?: throw IllegalStateException("Attempted to access UHCPlayer for a player that is not online")

    val team: UHCTeam?
        get() = plugin.game.teamManager.getTeam(playerUUID)

    val spectator: Boolean
        get() = plugin.game.teamManager.isSpectating(player)

    companion object : Listener {
        private val players = mutableMapOf<UUID, UHCPlayer>()
        lateinit var plugin: UHCPlugin

        fun get(player: Player): UHCPlayer {
            return players.computeIfAbsent(player.uniqueId) { UHCPlayer(plugin, player.uniqueId) }
        }

        @EventHandler
        fun logout(event: PlayerQuitEvent) {
            plugin.logger.info { "Removing UHCPlayer for ${event.player.name}" }
            players.remove(event.player.uniqueId)
        }
    }
}

val Player.uhcPlayer
    get() = UHCPlayer.get(this)