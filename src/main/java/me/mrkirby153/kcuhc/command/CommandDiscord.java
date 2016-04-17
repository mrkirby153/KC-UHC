package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.UUID;

public class CommandDiscord extends BaseCommand{
    private final String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to perform this command!");
            return true;
        }
        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("link")) {
            String code = generateCode(5);
            UHC.handler.setLinkCode(code, player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Please log into handler and in the uhc-link channel, enter the following command: ");
            player.sendMessage(ChatColor.AQUA + "!link " + code);
            return true;
        }
        if(args[0].equalsIgnoreCase("status")){
            BaseComponent statusMsg = UHC.handler.hasLinkedDiscord(player.getUniqueId())? UtilChat.generateBoldChat("You have linked discord", ChatColor.GREEN) :
                    UtilChat.generateBoldChat("You have not linked discord!", ChatColor.DARK_RED);
            player.spigot().sendMessage(statusMsg);
        }
        if(restrictAdmin(sender))
            return true;
        if(args[0].equalsIgnoreCase("cinit")){
            UHC.handler.initChannels();
        }
        if(args[0].equalsIgnoreCase("shutdown")){
            UHC.handler.shutdown();
        }
        if(args[0].equalsIgnoreCase("init")){
            UHC.handler.init();
        }
        if(args[0].equalsIgnoreCase("disperse")){
            UHC.handler.sendEveryoneToChannels();
        }
        if(args[0].equalsIgnoreCase("bringtolobby")){
            UHC.handler.bringEveryoneToLobby();
        }
        if(args[0].equalsIgnoreCase("purgechannels")){
            UHC.handler.cleanupTeams();
        }
        if(args[0].equalsIgnoreCase("linked")){
            ArrayList<UUID> players = new ArrayList<>();
            for(Player p : Bukkit.getOnlinePlayers()){
                players.add(p.getUniqueId());
            }
            ArrayList<UUID> linkedPlayers = UHC.handler.getAllLinkedPlayers();
            players.removeAll(linkedPlayers);
            for(UUID u : linkedPlayers){
                player.sendMessage(UtilChat.generateFormattedChat(Bukkit.getOfflinePlayer(u).getName()+" has linked to discord!", ChatColor.GREEN, 0).toLegacyText());
            }
            for(UUID u : players){
                player.sendMessage(UtilChat.generateFormattedChat(Bukkit.getOfflinePlayer(u).getName()+" has not linked discord!", ChatColor.RED, 0).toLegacyText());
            }
        }
        return true;
    }

    private String generateCode(int size) {
        StringBuilder sb = new StringBuilder();
        SecureRandom r = new SecureRandom();
        for (int i = 0; i < size; i++) {
            sb.append(validChars.charAt(r.nextInt(validChars.length())));
        }
        return sb.toString();
    }
}
