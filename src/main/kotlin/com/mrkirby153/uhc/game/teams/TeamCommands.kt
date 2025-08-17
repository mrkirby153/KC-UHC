package com.mrkirby153.uhc.game.teams

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import com.mrkirby153.uhc.game.UHCGame
import me.mrkirby153.kcutils.Chat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("team|teams")
class TeamCommands(private val game: UHCGame) : BaseCommand() {

    @Subcommand("create|add")
    @CommandPermission("kcuhc.team.create")
    fun createTeam(sender: CommandSender, name: String, color: TextColor) {
        game.teamManager.createTeam(name, color)
        sender.sendMessage(Chat.message("Team", "Team {name} created", "name" to name))
    }

    @Subcommand("remove")
    @CommandPermission("kcuhc.team.remove")
    fun removeTeam(sender: CommandSender, team: UHCTeam) {
        game.teamManager.deleteTeam(team)
        sender.sendMessage(Chat.message("Team", "Team {name} removed", "name" to team.name))
    }

    @Subcommand("join")
    @CommandPermission("kcuhc.team.join")
    @CommandCompletion("@teams")
    fun joinTeam(sender: CommandSender, team: UHCTeam, @Optional other: OfflinePlayer?) {
        if (other != null) {
            if (!sender.hasPermission("kcuhc.teams.join.others")) {
                sender.sendMessage(Chat.error("You do not have permission to perform that command"))
                return
            }
            game.teamManager.addPlayerToTeam(other.uniqueId, team)
            sender.sendMessage(
                Chat.message(
                    "Team",
                    "Added {player} to team {team}",
                    "player" to (other.name ?: "???"),
                    "team" to team.name
                )
            )
            Bukkit.getPlayer(other.uniqueId)?.sendMessage(
                Chat.message(
                    "Team",
                    "You have been assigned to team {team} by {sender}",
                    "team" to team.name,
                    "sender" to sender.name
                )
            )
        } else {
            if (sender !is Player) {
                sender.sendMessage(Chat.error("This command can only be used by players"))
                return
            }
            game.teamManager.addPlayerToTeam(sender.uniqueId, team)
            sender.sendMessage(Chat.message("Team", "Joined team {team}", "team" to team.name))
        }
    }

    @Subcommand("leave")
    @CommandPermission("kcuhc.team.leave")
    fun leaveTeam(sender: Player) {
        if (game.teamManager.getTeam(sender.uniqueId) == null) {
            sender.sendMessage(Chat.message("Team", "You are not on a team"))
            return
        }
        game.teamManager.removePlayerFromTeam(sender.uniqueId)
        sender.sendMessage(Chat.message("Team", "Left your current team"))
    }

    @Subcommand("list")
    @CommandPermission("kcuhc.team.list")
    fun listTeams(sender: CommandSender) {
        val teams = game.teamManager.getTeams()
        if (teams.isEmpty()) {
            sender.sendMessage(Chat.message("Team", "No teams available"))
            return
        }
        sender.sendMessage(
            Chat.formattedChat(
                "---------- Teams --------",
                NamedTextColor.GRAY,
            )
        )
        teams.forEach { team ->
            val component = Component.text("Name: ").color(NamedTextColor.GRAY)
                .append(Component.text(team.name).color(team.color))
                .append(Component.text(" | Members: ").color(NamedTextColor.GRAY))
                .append(Component.text(team.size).color(NamedTextColor.GOLD))
            sender.sendMessage(component)
        }
        sender.sendMessage(
            Chat.formattedChat(
                "--------------------------",
                NamedTextColor.GRAY,
            )
        )
    }

    @Subcommand("reload")
    @CommandPermission("kcuhc.team.reload")
    fun reload(sender: CommandSender) {
        game.teamManager.loadTeams()
        sender.sendMessage(Chat.message("Teams", "Reloaded teams"))
    }

    @Default
    @CommandPermission("kcuhc.teams.show")
    fun getTeam(player: Player) {
        val currentTeam = game.teamManager.getTeam(player.uniqueId)
        if (currentTeam == null) {
            player.sendMessage(Chat.message("Team", "You are not on a team"))
        } else {
            val members = currentTeam.getPlayers().joinToString(", ") { it.name ?: "???" }
            player.sendMessage(
                Chat.message(
                    "Team",
                    "You are on team {team} with {size} members: {members}",
                    "team" to currentTeam.name,
                    "size" to currentTeam.size.toString(),
                    "members" to members
                )
            )
        }
    }
}