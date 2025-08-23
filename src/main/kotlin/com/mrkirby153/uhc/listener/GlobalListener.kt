package com.mrkirby153.uhc.listener

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import com.mrkirby153.uhc.game.GameState
import com.mrkirby153.uhc.game.UHCGame
import io.papermc.paper.event.player.AsyncChatEvent
import me.mrkirby153.kcutils.Chat
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

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


    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        event.joinMessage(Chat.message("Join", "{player} joined!", "player" to event.player.name()))
        Bukkit.getOnlinePlayers().forEach { player ->
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        event.quitMessage(Chat.message("Quit", "{player} left!", "player" to event.player.name()))
        Bukkit.getOnlinePlayers().forEach { player ->
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val teamColor =
            game.teamManager.getTeam(event.player.uniqueId)?.color ?: NamedTextColor.WHITE
        event.player.displayName(event.player.displayName().color(teamColor))
    }
}