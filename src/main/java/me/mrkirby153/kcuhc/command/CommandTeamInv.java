package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CommandTeamInv extends BaseCommand {

    private TeamHandler teamHandler;
    private UHC plugin;

    public CommandTeamInv(TeamHandler teamHandler, UHC plugin) {
        this.teamHandler = teamHandler;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (restrictPlayer(sender))
            return true;
        Player player = (Player) sender;
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("item")) {
                boolean spaceFree = false;
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    if(i == 36 || i == 37 || i == 38 || i == 39 || i == 40)
                        continue;
                    if (player.getInventory().getItem(i) == null) {
                        spaceFree = true;
                        break;
                    }
                }
                if (!spaceFree) {
                    player.sendMessage(UtilChat.generateLegacyError("There is no space in your inventory!"));
                } else {
                    plugin.arena.getTeamInventoryHandler().giveInventoryItem(player);
                    player.sendMessage(UtilChat.message("Given you a team inventory item!"));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("clear")) {
                plugin.arena.getTeamInventoryHandler().takeInventoryItem(player);
                player.sendMessage(UtilChat.message("Taken the team inventory item. Use " + ChatColor.GOLD + "/teaminv item" + ChatColor.GRAY + " to get it back"));
                return true;
            }
        }
        UHCTeam team = teamHandler.getTeamForPlayer(player);

        if (team == null || !(team instanceof UHCPlayerTeam)) {
            sender.sendMessage(UtilChat.generateLegacyError("You are not on a team!"));
            return true;
        }
        if (!plugin.arena.getProperties().TEAM_INV_ENABLED.get()) {
            sender.sendMessage(UtilChat.generateLegacyError("Team inventories are not enabled!"));
            return true;
        }
        if (plugin.arena.currentState() != UHCArena.State.RUNNING) {
            sender.sendMessage(UtilChat.generateLegacyError("You cannot open a team inventory before the game starts!"));
            return true;
        }
        Inventory inventory = plugin.arena.getTeamInventoryHandler().getInventory(team);
        player.openInventory(inventory);
        return true;
    }
}
