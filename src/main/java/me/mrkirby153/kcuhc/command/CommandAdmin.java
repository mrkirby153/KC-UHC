package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTime;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class CommandAdmin extends BaseCommand {

    private HashMap<UUID, Long> nextMessageIn = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (restrictPlayer(commandSender))
            return true;
        if (args.length == 0) {
            commandSender.sendMessage(UtilChat.generateLegacyError("Please provide a message!"));
        }
        if (args[0].equalsIgnoreCase("ban") && UHC.admins.contains(commandSender.getName()) && args.length == 3) {
            Player p = Bukkit.getPlayer(args[1]);
            if (p == null) {
                commandSender.sendMessage(UtilChat.generateLegacyError("We could not find that player"));
                return true;
            }
            long banTime = 1000 * Long.parseLong(args[2]);
            nextMessageIn.put(p.getUniqueId(), System.currentTimeMillis() + banTime);
            commandSender.sendMessage(ChatColor.GREEN + "Denied " + p.getName() + " access to /a for " + ChatColor.GOLD + UtilTime.format(1, banTime, UtilTime.TimeUnit.FIT));
            return true;
        }
        if (Bukkit.getPlayer(args[0]) != null && UHC.admins.contains(commandSender.getName())) {
            String response = "";
            for (int i = 1; i < args.length; i++) {
                response += args[i] + " ";
            }
            response = ChatColor.RESET + commandSender.getName() + ": " + ChatColor.AQUA + response;
            Bukkit.getPlayer(args[0]).sendMessage(ChatColor.DARK_PURPLE + "> " + response);
            commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "< " + response);
            Player player = Bukkit.getPlayer(args[0]);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HARP, 1F, 1F);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HARP, 1F, 1.5F);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HARP, 1F, 12F);
            return true;
        }
        Long nextMessageIn = this.nextMessageIn.get(((Player) commandSender).getUniqueId());
        if (nextMessageIn != null) {
            long msLeft = nextMessageIn - System.currentTimeMillis();
            if (msLeft > 0) {
                commandSender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You are doing that too much! Try again in " + ChatColor.DARK_RED + ChatColor.BOLD + UtilTime.format(1, msLeft, UtilTime.TimeUnit.FIT));
                return true;
            }
        }
        String msg = "";
        for (String arg : args) {
            msg += arg + " ";
        }
        msg = commandSender.getName() + ": " + ChatColor.RESET + msg.trim();
        UHCTeam team = TeamHandler.getTeamForPlayer((Player) commandSender);
        if (team != null) {
            msg = team.getColor() + "(" + team.getFriendlyName() + ") " + msg;
        }
        commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "< " + msg);
        sendToAdmin(msg);
        this.nextMessageIn.put(((Player) commandSender).getUniqueId(), System.currentTimeMillis() + (5000));
        return true;
    }


    public static void sendToAdmin(String msg) {
        for (String s : UHC.admins) {
            Player player = Bukkit.getPlayer(s);
            if (player != null) {
                player.sendMessage(ChatColor.DARK_PURPLE + "> " + msg);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HARP, 1F, 1F);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HARP, 1F, 1.5F);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HARP, 1F, 12F);
            }
        }
    }
}
