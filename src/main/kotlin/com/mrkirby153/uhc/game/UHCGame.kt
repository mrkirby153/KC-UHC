package com.mrkirby153.uhc.game

import com.mrkirby153.uhc.UHCPlugin
import com.mrkirby153.uhc.game.teams.TeamManager
import org.bukkit.event.Event

class UHCGame(val plugin: UHCPlugin) {

    val teamManager = TeamManager(this)

    fun init() {
        plugin.logger.info("Initializing UHC Game")
        teamManager.loadTeams()
    }

    fun shutdown() {

    }


    fun dispatchEvent(event: Event) {
        plugin.server.pluginManager.callEvent(event)
    }
}