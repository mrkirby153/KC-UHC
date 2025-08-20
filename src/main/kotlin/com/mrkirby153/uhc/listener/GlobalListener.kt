package com.mrkirby153.uhc.listener

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import com.mrkirby153.uhc.game.GameState
import com.mrkirby153.uhc.game.UHCGame
import me.mrkirby153.kcutils.Chat
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class GlobalListener(private val game: UHCGame) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onPaperServerListPing(event: PaperServerListPingEvent) {
        var motd =
            Chat.formattedChat(
                "${game.plugin.gameTitle}:\n",
                NamedTextColor.YELLOW
            )
        motd = motd.append(
            Chat.formattedChat(
                when (game.state) {
                    GameState.WAITING -> "Waiting for players"
                    GameState.COUNTDOWN -> "Starting"
                    GameState.RUNNING -> "In Progress"
                    GameState.ENDED -> "Ended"
                },
                when (game.state) {
                    GameState.WAITING -> NamedTextColor.GRAY
                    GameState.COUNTDOWN -> NamedTextColor.YELLOW
                    GameState.RUNNING -> NamedTextColor.GREEN
                    GameState.ENDED -> NamedTextColor.GRAY
                }
            )
        )
        event.motd(motd)
    }
}