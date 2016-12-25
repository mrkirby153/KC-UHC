package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.player.TeamInventoryModule;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        if (!ModuleRegistry.isLoaded(TeamInventoryModule.class)) {
            sender.sendMessage(UtilChat.generateLegacyError("Team inventories aren't enabled!"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("item")) {
                ModuleRegistry.getLoadedModule(TeamInventoryModule.class).ifPresent(i -> i.giveInventoryItem(player));
                player.sendMessage(UtilChat.message("Given you a team inventory item!"));
                return true;
            }
            if (args[0].equalsIgnoreCase("clear")) {
                ModuleRegistry.getLoadedModule(TeamInventoryModule.class).ifPresent(i -> i.takeInventoryItem(player));
                player.sendMessage(UtilChat.message("Taken the team inventory item. Use " + ChatColor.GOLD + "/teaminv item" + ChatColor.GRAY + " to get it back"));
                return true;
            }
        }
        UHCTeam team = teamHandler.getTeamForPlayer(player);

        if (team == null || !(team instanceof UHCPlayerTeam)) {
            sender.sendMessage(UtilChat.generateLegacyError("You are not on a team!"));
            return true;
        }
        if (!ModuleRegistry.isLoaded(TeamInventoryModule.class)) {
            sender.sendMessage(UtilChat.generateLegacyError("Team inventories are not enabled!"));
            return true;
        }
        if (plugin.arena.currentState() != UHCArena.State.RUNNING) {
            sender.sendMessage(UtilChat.generateLegacyError("You cannot open a team inventory before the game starts!"));
            return true;
        }
        ModuleRegistry.getLoadedModule(TeamInventoryModule.class).ifPresent(i -> player.openInventory(i.getInventory(team)));
        return true;
    }
}
