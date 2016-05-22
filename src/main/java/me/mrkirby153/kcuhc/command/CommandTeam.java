package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class CommandTeam extends BaseCommand {

    private boolean assignSelf = true;

    @Override
    @SuppressWarnings("unchecked")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (restrictPlayer(sender))
                return true;
            Player p = (Player) sender;
            UHCTeam teamForPlayer = TeamHandler.getTeamForPlayer(p);
            //
            if (teamForPlayer != null) {
                p.spigot().sendMessage(UtilChat.generateBoldChat("You are on team " + teamForPlayer.getName(), ChatColor.GREEN));
            } else {
                p.spigot().sendMessage(UtilChat.generateBoldChat("You are not on any team!", ChatColor.RED));
            }
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                for (UHCTeam t : TeamHandler.teams()) {
                    if (t.getName().equalsIgnoreCase(TeamHandler.SPECTATORS_TEAM))
                        continue;
                    sender.sendMessage(t.getName());
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("manualassign")) {
                if (restrictAdmin(sender)) {
                    return true;
                }
                assignSelf = !assignSelf;
                if (assignSelf) {
                    sender.sendMessage(UtilChat.generateFormattedChat("Players can now assign their own teams", ChatColor.GREEN, 8).toLegacyText());
                } else {
                    sender.sendMessage(UtilChat.generateFormattedChat("Players can no longer assign their own teams", ChatColor.RED, 8).toLegacyText());
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("save")) {
                if (restrictAdmin(sender))
                    return true;
                TeamHandler.saveToFile();
                sender.sendMessage("Saved");
                return true;
            }
            if (args[0].equalsIgnoreCase("delAll")) {
                if (restrictAdmin(sender))
                    return true;
                TeamHandler.unregisterAll();
                TeamHandler.loadFromFile();
                sender.sendMessage("Removed all teams");
                return true;
            }
            if (args[0].equalsIgnoreCase("spread")) {
                if (restrictAdmin(sender))
                    return true;
                UHC.arena.distributeTeams(50);
                return true;
            }
            if (args[0].equalsIgnoreCase("load")) {
                if (restrictAdmin(sender))
                    return true;
                TeamHandler.loadFromFile();
                sender.sendMessage("Loaded");
                return true;
            }
            if (!(UHC.arena.currentState() == UHCArena.State.WAITING || UHC.arena.currentState() == UHCArena.State.INITIALIZED)) {
                if (sender instanceof Player) {
                    sender.sendMessage(UtilChat.generateLegacyError("You cannot change teams when the game has started!"));
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("assign")) {
                if (restrictAdmin(sender))
                    return true;
                int playerCount = 0;
                int teamCount = 0;
                TeamHandler.loadFromFile();
                for (UHCTeam t : TeamHandler.teams()) {
                    if (t == TeamHandler.getTeamByName(TeamHandler.SPECTATORS_TEAM))
                        continue;
                    ArrayList<UUID> uuuids = (ArrayList<UUID>) t.getPlayers().clone();
                    for (UUID u : uuuids) {
                        Player p = Bukkit.getPlayer(u);
                        if (p != null) {
                            TeamHandler.leaveTeam(p);
                            TeamHandler.joinTeam(t, p);
                            UHC.arena.addPlayer(p);
                            playerCount++;
                        }
                    }
                    teamCount++;
                }
                sender.sendMessage(UtilChat.generateFormattedChat("Assigned " + playerCount + " player(s) to " + teamCount + " teams", ChatColor.GREEN, 0).toLegacyText());
                return true;
            }
            if (restrictPlayer(sender))
                return true;
            String teamToJoin = args[0];
            Player p = (Player) sender;
            if (!assignSelf && !UHC.isAdmin(p)) {
                p.spigot().sendMessage(UtilChat.generateError("You cannot assign your team. Please wait for it to be assigned for you"));
                return true;
            }
            if (args[0].equalsIgnoreCase("leave")) {
                TeamHandler.leaveTeam(p);
            }
            if (teamToJoin.equalsIgnoreCase("spectators")) {
                p.spigot().sendMessage(UtilChat.generateError("Use /spectate to join the spectators team!"));
                return true;
            }
            TeamHandler.leaveTeam(p);
            UHCTeam teamByName = TeamHandler.getTeamByName(teamToJoin);
            if (teamByName == null) {
                p.spigot().sendMessage(UtilChat.generateError("That team does not exist!"));
                return true;
            }
            TeamHandler.joinTeam(teamByName, p);
            return true;
        }
        if (args.length == 2) {
            /// --- END TEMP
            if (args[0].equalsIgnoreCase("remove")) {
                UHCTeam teamByName = TeamHandler.getTeamByName(args[1]);
                if (teamByName == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That team does not exist!"));
                    return true;
                }
                ArrayList<UUID> players = (ArrayList<UUID>) teamByName.getPlayers().clone();
                for (UUID u : players) {
                    Player p = Bukkit.getPlayer(u);
                    if (p == null)
                        continue;
                    TeamHandler.leaveTeam(p);
                }
                TeamHandler.unregisterTeam(teamByName);
                sender.sendMessage(UtilChat.generateFormattedChat("Removed team!", ChatColor.GREEN, 0).toLegacyText());
                return true;
            }
            Player toAdd = Bukkit.getPlayer(args[0]);
            if (toAdd == null) {
                sender.sendMessage(UtilChat.generateLegacyError("That player is not online!"));
                return true;
            }
            UHCTeam team = TeamHandler.getTeamByName(args[1]);
            if (team == null) {
                sender.sendMessage(UtilChat.generateLegacyError("That team does not exist!"));
                return true;
            }
            TeamHandler.joinTeam(team, toAdd);
            return true;
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("add")) {
                if (restrictAdmin(sender))
                    return true;
                String teamName = args[1];
                if (TeamHandler.getTeamByName(teamName) != null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That team already exists!"));
                    return true;
                }
                ChatColor color;
                try {
                    color = ChatColor.valueOf(args[2]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("That chat color does not exist!"));
                    return true;
                }
                UHC.arena.newTeam(teamName, color);
                sender.sendMessage(UtilChat.generateFormattedChat("Added team!", ChatColor.GREEN, 0).toLegacyText());
                return true;
            }
        }
        return false;
    }
}
