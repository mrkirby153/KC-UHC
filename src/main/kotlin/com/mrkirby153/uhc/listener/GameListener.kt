package com.mrkirby153.uhc.listener

import com.mrkirby153.uhc.game.UHCGame
import me.mrkirby153.kcutils.Chat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import java.time.Duration

class GameListener(private val game: UHCGame) : Listener {


    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val deathMessage = event.deathMessage()
        val killer = event.entity.killer

        if (deathMessage != null) {
            val newMessage = deathMessage.replaceText {
                it.match(event.entity.name)
                    .replacement(Chat.formattedChat(event.entity.name, NamedTextColor.GOLD))
            }.replaceText {
                val killerName = killer?.name ?: return@replaceText
                it.match(killerName)
                    .replacement(Chat.formattedChat(event.entity.name, NamedTextColor.GOLD))
            }
            event.deathMessage(newMessage)
        }

        val teamColor =
            game.teamManager.getTeam(event.player.uniqueId)?.color ?: NamedTextColor.WHITE
        val deathTitle = Component.text("[").color(NamedTextColor.WHITE)
            .append(Component.text("x").color(NamedTextColor.RED))
            .append(Component.text("] ").color(NamedTextColor.WHITE))
            .append(Component.text(event.entity.name).color(teamColor))

        killer?.showTitle(
            Title.title(
                Component.text().build(),
                deathTitle,
                Title.Times.times(
                    Duration.ofMillis(250),
                    Duration.ofMillis(1000),
                    Duration.ofMillis(250)
                )
            )
        )
    }
}