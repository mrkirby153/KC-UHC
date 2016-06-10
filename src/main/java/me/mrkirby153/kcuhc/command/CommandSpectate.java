package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCArena;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSpectate extends BaseCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (restrictPlayer(sender))
            return true;
        Player player = (Player) sender;
        if (UHC.arena.currentState() == UHCArena.State.WAITING || UHC.arena.currentState() == UHCArena.State.INITIALIZED)
            if (TeamHandler.getTeamForPlayer(player) != TeamHandler.getTeamByName(TeamHandler.SPECTATORS_TEAM)) {
                TeamHandler.leaveTeam(player);
                TeamHandler.joinTeam(TeamHandler.spectatorsTeam(), player);
//                player.spigot().sendMessage(UtilChat.generateFormattedChat("You are now a spectator. Type /spectate to leave", ChatColor.RED, 0));
                player.sendMessage(UtilChat.message("You are now a spectator. Type " + ChatColor.GOLD + "/spectate " + ChatColor.GRAY + " to leave"));
            } else {
                TeamHandler.leaveTeam(player);
                player.teleport(UHC.arena.getCenter().add(0, 2, 0));
                player.sendMessage(UtilChat.message("You are no longer a spectator"));
            }
        else
            player.spigot().sendMessage(UtilChat.generateError("You cannot join/leave the spectators team because the game has already started!"));
        return true;
    }
}
