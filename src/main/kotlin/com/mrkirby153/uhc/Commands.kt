package com.mrkirby153.uhc

import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.PaperCommandManager
import com.mrkirby153.uhc.game.teams.TeamCommands
import com.mrkirby153.uhc.game.teams.UHCTeam
import me.mrkirby153.kcutils.Chat
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

class Commands(private val plugin: UHCPlugin, private val manager: PaperCommandManager) {


    fun register() {
        plugin.logger.info { "Registering commands" }

        this.registerCommandCompletions()
        this.registerResolvers()

        manager.registerCommand(TeamCommands(plugin.game))
        plugin.logger.info { "Commands registered" }
    }

    private fun registerCommandCompletions() {
        manager.commandCompletions.registerCompletion("teams") {
            plugin.game.teamManager.getTeams().map { it.name }
        }
    }

    private fun registerResolvers() {
        manager.commandContexts.registerContext(UHCTeam::class.java) { ctx ->
            val name = mutableListOf<String>()
            var current = ctx.popFirstArg() ?: return@registerContext null
            do {
                name.add(current)
                val potential = name.joinToString(" ")
                val team = plugin.game.teamManager.getTeamByName(potential)
                if (team != null) {
                    return@registerContext team
                }
                current = ctx.popFirstArg()
            } while (ctx.args.isNotEmpty())
            return@registerContext null
        }
        manager.commandContexts.registerContext(TextColor::class.java) { ctx ->
            val color = ctx.popFirstArg() ?: return@registerContext null
            // Check for named colors
            if (color.startsWith("#")) {
                // Parse hex color
                return@registerContext TextColor.fromHexString(color)
            }
            val namedColor = NamedTextColor.NAMES.value(color.lowercase())
            if (namedColor != null) {
                return@registerContext namedColor
            } else {
                ctx.issuer.issuer.sendMessage(Chat.message("Error", "Invalid color: $color"))
                throw InvalidCommandArgument(false)
            }
        }
    }
}