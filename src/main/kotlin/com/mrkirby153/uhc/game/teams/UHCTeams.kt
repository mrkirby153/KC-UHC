package com.mrkirby153.uhc.game.teams

import com.mrkirby153.uhc.BukkitEvent
import com.mrkirby153.uhc.game.UHCGame
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.OfflinePlayer
import java.util.UUID

/**
 * Class representing a team in a UHC game
 */
class UHCTeam(private val game: UHCGame, val name: String, val color: Color) {
    val id = UUID.randomUUID()
    private val members = mutableSetOf<UUID>()

    /**
     * Adds the given [player] to this team
     */
    fun addPlayer(player: OfflinePlayer) {
        addPlayer(player.uniqueId)
    }

    /**
     * Adds the given [uuid] to this team
     */
    fun addPlayer(uuid: UUID) {
        val player = Bukkit.getOfflinePlayer(uuid)
        members.add(uuid)
        game.plugin.server.pluginManager.callEvent(TeamJoinEvent(player))
    }

    /**
     * Removes the given [player] from this team
     */
    fun removePlayer(player: OfflinePlayer) {
        removePlayer(player.uniqueId)
    }

    /**
     * Removes the given [uuid] from this team
     */
    fun removePlayer(uuid: UUID) {
        val player = Bukkit.getOfflinePlayer(uuid)
        members.remove(uuid)
        game.plugin.server.pluginManager.callEvent(TeamPartEvent(player))
    }

    override fun equals(other: Any?): Boolean {
        return other is UHCTeam && other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun getPlayers() = members.map { Bukkit.getOfflinePlayer(it) }
}


data class TeamJoinEvent(val player: OfflinePlayer) : BukkitEvent()
data class TeamPartEvent(val player: OfflinePlayer) : BukkitEvent()