package com.mrkirby153.uhc.game

import com.mrkirby153.uhc.UHCPlugin
import com.mrkirby153.uhc.game.teams.TeamManager

class UHCGame(val plugin: UHCPlugin) {

    val teamManager = TeamManager(this)

    fun init() {
        plugin.logger.info("Initializing UHC Game")
    }

    fun shutdown() {

    }
}