package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class BaseCommand implements CommandExecutor{

    protected boolean isNumber(String string) {
        try {
            Double.parseDouble(string);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    protected boolean restrictAdmin(CommandSender sender){
        if (!UHC.isAdmin(sender.getName())) {
            if(!sender.isOp()) {
                sender.sendMessage(UtilChat.generateError("You must be an admin to perform that!").toLegacyText());
                return true;
            }
        }
        return false;
    }

    protected boolean restrictPlayer(CommandSender sender){
        if(!(sender instanceof Player)){
            sender.sendMessage(UtilChat.generateError("You must be a player to do that!").toLegacyText());
            return true;
        }
        return false;
    }
}
