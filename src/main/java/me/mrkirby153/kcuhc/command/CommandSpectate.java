package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.TeamSpectator;
import me.mrkirby153.kcuhc.utils.UtilChat;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSpectate extends BaseCommand {

    private TeamHandler teamHandler;
    private UHC plugin;

    public CommandSpectate(TeamHandler teamHandler, UHC plugin) {
        this.teamHandler = teamHandler;
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (restrictPlayer(sender))
            return true;
        Player player = (Player) sender;
        if (plugin.arena.currentState() == UHCArena.State.WAITING || plugin.arena.currentState() == UHCArena.State.INITIALIZED)
            if (!(teamHandler.getTeamForPlayer(player) instanceof TeamSpectator)) {
                teamHandler.leaveTeam(player);
                teamHandler.joinTeam(teamHandler.spectatorsTeam(), player);
//                player.spigot().sendMessage(UtilChat.generateFormattedChat("You are now a spectator. Type /spectate to leave", ChatColor.RED, 0));
                player.sendMessage(UtilChat.message("You are now a spectator. Type " + ChatColor.GOLD + "/spectate " + ChatColor.GRAY + " to leave"));
            } else {
                teamHandler.leaveTeam(player);
                player.teleport(plugin.arena.getCenter().add(0, 2, 0));
                player.sendMessage(UtilChat.message("You are no longer a spectator"));
            }
        else
            player.spigot().sendMessage(UtilChat.generateError("You cannot join/leave the spectators team because the game has already started!"));
        return true;
    }
}
