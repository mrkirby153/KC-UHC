package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CommandTeamInv extends BaseCommand{

    private TeamHandler teamHandler;

    public CommandTeamInv(TeamHandler teamHandler) {
        this.teamHandler = teamHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(restrictPlayer(sender))
            return true;
        Player player = (Player) sender;
        UHCTeam team = teamHandler.getTeamForPlayer(player);

        if(team == null || !(team instanceof UHCPlayerTeam)){
            sender.sendMessage(UtilChat.generateLegacyError("You are not on a team!"));
            return true;
        }
        if(!UHC.arena.getProperties().TEAM_INV_ENABLED.get()){
            sender.sendMessage(UtilChat.generateLegacyError("Team inventories are not enabled!"));
            return true;
        }
        if(UHC.arena.currentState() != UHCArena.State.RUNNING){
            sender.sendMessage(UtilChat.generateLegacyError("You cannot open a team inventory before the game starts!"));
            return true;
        }
        Inventory inventory = UHC.arena.getTeamInventoryHandler().getInventory(team);
        player.openInventory(inventory);
        return true;
    }
}
