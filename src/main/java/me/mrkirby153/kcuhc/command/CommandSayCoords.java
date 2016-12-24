package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class CommandSayCoords extends BaseCommand {

    private TeamHandler teamHandler;

    public CommandSayCoords(TeamHandler teamHandler) {
        this.teamHandler = teamHandler;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (restrictPlayer(commandSender))
            return true;
        Player player = (Player) commandSender;
        UHCTeam team = teamHandler.getTeamForPlayer(player);
        if (team == null || !(team instanceof UHCPlayerTeam)) {
            player.sendMessage(UtilChat.generateLegacyError("You are not on a team!"));
            return true;
        }
        team.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F);
            Location l = player.getLocation();
            if (p.getLocation().getWorld() != l.getWorld()) {
                p.sendMessage(UtilChat.message(ChatColor.GOLD + commandSender.getName() + ChatColor.GRAY + " is in another dimension" + ChatColor.GOLD + "(" + l.getWorld().getName() + ")"));
            }
            p.sendMessage(UtilChat.message(ChatColor.GOLD + commandSender.getName() + ChatColor.GRAY + " is currently at " + ChatColor.GOLD + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() +
                    ChatColor.GRAY + " (" + UtilTime.trim(1, p.getLocation().distance(l)) + ChatColor.GRAY + " blocks from you)"));
        });
        return true;
    }
}
