package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class CommandTeam extends BaseCommand {

    private boolean assignSelf = true;
    
    private final TeamHandler teamHandler;
    private UHC plugin;

    public CommandTeam(TeamHandler teamHandler, UHC plugin){
        this.teamHandler = teamHandler;
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (restrictPlayer(sender))
                return true;
            Player p = (Player) sender;
            UHCTeam teamForPlayer = teamHandler.getTeamForPlayer(p);
            //
            if (teamForPlayer != null) {
                p.sendMessage(UtilChat.message("You are on team " + ChatColor.GOLD + teamForPlayer.getTeamName()));
            } else {
                p.sendMessage(UtilChat.message("You are not on any team"));
            }
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                for (UHCTeam t : teamHandler.teams()) {
                    String teamMembers = "";
                    for (UUID u : t.getPlayers()) {
                        boolean online = Bukkit.getPlayer(u) != null;
                        String name = Bukkit.getOfflinePlayer(u).getName();
                        teamMembers += (online ? ChatColor.GREEN : ChatColor.RED) + name + ChatColor.RESET + ", ";
                    }
                    if (teamMembers.length() > 2)
                        teamMembers = teamMembers.substring(0, teamMembers.length() - 2);
                    else
                        teamMembers = ChatColor.RED + "Nobody on team";
//                    sender.sendMessage(t.getColor() + " + " + t.getName() + " [" + t.getFriendlyName() + "] (" + t.getPlayers().size() + "): " + teamMembers);
                    sender.sendMessage(UtilChat.message("Team " + ChatColor.GOLD + t.getTeamName() + ChatColor.DARK_GRAY + " [" +
                            ChatColor.AQUA + t.getFriendlyName() + ChatColor.DARK_GRAY + "] (" + ChatColor.GOLD +
                            t.getPlayers().size() + ChatColor.DARK_GRAY + ") [" + ChatColor.YELLOW + teamMembers + ChatColor.DARK_GRAY + "]"));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("manualassign")) {
                if (restrictAdmin(sender)) {
                    return true;
                }
                assignSelf = !assignSelf;
                if (assignSelf) {
                    sender.sendMessage(UtilChat.message("Players can now assign their own teams"));
                } else {
                    sender.sendMessage(UtilChat.message("Players can no longer assign their own team"));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("save")) {
                if (restrictAdmin(sender))
                    return true;
                teamHandler.saveToFile();
                sender.sendMessage("Saved");
                return true;
            }
            if (args[0].equalsIgnoreCase("delAll")) {
                if (restrictAdmin(sender))
                    return true;
                teamHandler.unregisterAll();
                sender.sendMessage(UtilChat.message("Removed all teams and loaded teams"));
                return true;
            }
            if (args[0].equalsIgnoreCase("spread")) {
                if (restrictAdmin(sender))
                    return true;
                plugin.arena.distributeTeams(50);
                sender.sendMessage(UtilChat.message("Spread teams!"));
                return true;
            }
            if (args[0].equalsIgnoreCase("load")) {
                if (restrictAdmin(sender))
                    return true;
                teamHandler.loadFromFile();
                sender.sendMessage(UtilChat.message("Loaded teams from file"));
                return true;
            }
            if (!(plugin.arena.currentState() == UHCArena.State.WAITING || plugin.arena.currentState() == UHCArena.State.INITIALIZED)) {
                if (sender instanceof Player) {
                    sender.sendMessage(UtilChat.generateLegacyError("You cannot change teams when the game has started!"));
                    return true;
                }
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
                teamHandler.leaveTeam(p);
                p.sendMessage(UtilChat.message("You have left your team"));
            }
            if (teamToJoin.equalsIgnoreCase("spectators")) {
                p.spigot().sendMessage(UtilChat.generateError("Use /spectate to join the spectators team!"));
                return true;
            }
            teamHandler.leaveTeam(p);
            UHCTeam teamByName = teamHandler.getTeamByName(teamToJoin);
            if (teamByName == null) {
                p.spigot().sendMessage(UtilChat.generateError("That team does not exist!"));
                return true;
            }
            teamHandler.joinTeam(teamByName, p);
            return true;
        }
        if (args.length == 2) {
            /// --- END TEMP
            if (args[0].equalsIgnoreCase("remove")) {
                if(restrictAdmin(sender))
                    return true;
                UHCTeam teamByName = teamHandler.getTeamByName(args[1]);
                if (teamByName == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That team does not exist!"));
                    return true;
                }
                ArrayList<UUID> players = new ArrayList<>(teamByName.getPlayers());
                for (UUID u : players) {
                    Player p = Bukkit.getPlayer(u);
                    if (p == null)
                        continue;
                    teamHandler.leaveTeam(p);
                }
                if(!(teamByName instanceof UHCPlayerTeam)){
                    sender.sendMessage(UtilChat.generateLegacyError("You cannot remove non-player teams!"));
                    return true;
                }
                teamHandler.unregisterTeam(teamByName);
                sender.sendMessage(UtilChat.message("Removed team " + ChatColor.GOLD + args[1]));
                return true;
            }
            Player toAdd = Bukkit.getPlayer(args[0]);
            if (toAdd == null) {
                sender.sendMessage(UtilChat.generateLegacyError("That player is not online!"));
                return true;
            }
            UHCTeam team = teamHandler.getTeamByName(args[1]);
            if (team == null) {
                sender.sendMessage(UtilChat.generateLegacyError("That team does not exist!"));
                return true;
            }
            teamHandler.joinTeam(team, toAdd);
            return true;
        }
        if (args.length > 2) {
            if (args[0].equalsIgnoreCase("teamname")) {
                String team = args[1];
                String name = "";
                for (int i = 2; i < args.length; i++) {
                    name += args[i] + " ";
                }
                name = name.trim();
                UHCTeam teamByName = teamHandler.getTeamByName(team);
                if (teamByName == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That team does not exist!"));
                    return true;
                }
                teamByName.setFriendlyName(name);
                teamHandler.saveToFile();
                sender.sendMessage(UtilChat.message("Set team " + ChatColor.GOLD + args[1] + "'s " + ChatColor.GRAY + "name to " + ChatColor.GOLD + name));
                return true;
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("add")) {
                if (restrictAdmin(sender))
                    return true;
                String teamName = args[1];
                if (teamHandler.getTeamByName(teamName) != null) {
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
                teamHandler.registerTeam(new UHCPlayerTeam(teamName, color));
                sender.sendMessage(UtilChat.generateFormattedChat("Added team!", ChatColor.GREEN, 0).toLegacyText());
                sender.sendMessage(UtilChat.message("Created team " + ChatColor.GOLD + teamName + ChatColor.GRAY + " (" + color + args[2] + ChatColor.GRAY + ")"));
                return true;
            }
        }
        return false;
    }
}
