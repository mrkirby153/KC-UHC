package com.mrkirby153.uhc.game.teams

import com.mrkirby153.uhc.BukkitEvent
import com.mrkirby153.uhc.game.UHCGame
import net.kyori.adventure.text.format.TextColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID

typealias UhcTeamId = UUID
typealias PlayerUuid = UUID

class TeamManager(private val game: UHCGame) {
    private val teams = mutableMapOf<UhcTeamId, UHCTeam>()
    private val teamMap = mutableMapOf<PlayerUuid, UHCTeam>()

    private val spectators = mutableSetOf<PlayerUuid>()

    private val dataFile = File(game.plugin.dataFolder, "teams.yml")

    fun createTeam(name: String, color: TextColor): UHCTeam {
        val team = UHCTeam(game, name, color)
        teams[team.id] = team
        saveTeams()
        return team
    }

    fun deleteTeam(uhcTeam: UHCTeam) {
        uhcTeam.getPlayers().forEach { removePlayerFromTeam(it.uniqueId) }
        teams.remove(uhcTeam.id)
        saveTeams()
    }

    fun removePlayerFromTeam(player: PlayerUuid) {
        val team = teamMap.remove(player)
        team?.removePlayer(player)
        saveTeams()
    }

    /**
     * Adds the given [player] to the specified [team]. If the player is already on a team, it will
     * swap them to the new team
     */
    fun addPlayerToTeam(player: PlayerUuid, team: UHCTeam) {
        check(team.id in teams) { "Team is not registered" }
        val previousTeam = teamMap[player]
        previousTeam?.removePlayer(player)
        team.addPlayer(player)
        teamMap[player] = team
        saveTeams()
    }

    fun getTeamById(id: UhcTeamId): UHCTeam? {
        return teamMap[id]
    }

    fun isSpectating(player: Player): Boolean {
        return player.uniqueId in spectators
    }

    fun addSpectator(player: Player) {
        spectators.add(player.uniqueId)
        game.dispatchEvent(PlayerSpectateEvent(player))
    }

    fun removeSpectator(player: Player) {
        spectators.remove(player.uniqueId)
        game.dispatchEvent(PlayerStopSpectatingEvent(player))
    }


    fun saveTeams() {
        if (!dataFile.exists()) {
            dataFile.createNewFile()
        }
        val config = YamlConfiguration()
        teams.forEach { (teamId, team) ->
            val teamSection = config.createSection(teamId.toString())
            teamSection.set("name", team.name)
            teamSection.set("color", team.color.asHexString())
            teamSection.set("members", team.getPlayers().map { it.uniqueId.toString() }.toList())
        }
        config.save(dataFile)
    }

    fun loadTeams() {
        if (!dataFile.exists())
            return
        val config = YamlConfiguration.loadConfiguration(dataFile)
        this.teams.clear()
        config.getKeys(false).forEach { key ->
            val teamId = UUID.fromString(key)
            val section = config.getConfigurationSection(key) ?: return@forEach
            val name = section.getString("name") ?: return@forEach
            val color = TextColor.fromHexString(section.getString("color")!!)!!
            val members = section.getStringList("members").map { UUID.fromString(it) }
            val team = UHCTeam(game, name, color, teamId)
            teams[teamId] = team
            members.forEach {
                addPlayerToTeam(it, team)
            }
        }
    }

    fun getTeams() = teams.values.toList()

    fun getTeamByName(name: String): UHCTeam? {
        return teams.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun getTeam(player: PlayerUuid): UHCTeam? {
        return teamMap[player]
    }
}

data class PlayerSpectateEvent(val player: Player) : BukkitEvent()
data class PlayerStopSpectatingEvent(val player: Player) : BukkitEvent()