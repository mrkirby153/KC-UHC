package com.mrkirby153.uhc.game.teams

import com.mrkirby153.uhc.game.UHCGame
import org.bukkit.Color
import java.util.UUID

class TeamManager(private val game: UHCGame) {
    private val teams = mutableMapOf<UUID, UHCTeam>()
    private val teamMap = mutableMapOf<UUID, UHCTeam>()

    fun createTeam(name: String, color: Color): UHCTeam {
        val team = UHCTeam(game, name, color)
        teams[team.id] = team
        return team
    }

    fun deleteTeam(uhcTeam: UHCTeam) {
        uhcTeam.getPlayers().forEach { removePlayerFromTeam(it.uniqueId) }
        teams.remove(uhcTeam.id)
    }

    fun removePlayerFromTeam(player: UUID) {
        val team = teamMap.remove(player)
        team?.removePlayer(player)
    }

    /**
     * Adds the given [player] to the specified [team]. If the player is already on a team, it will
     * swap them to the new team
     */
    fun addPlayerToTeam(player: UUID, team: UHCTeam) {
        check(team.id in teams) { "Team is not registered" }
        val previousTeam = teamMap[player]
        previousTeam?.removePlayer(player)
        team.addPlayer(player)
        teamMap[player] = team
    }

    fun getTeamById(id: UUID): UHCTeam? {
        return teamMap[id]
    }
}