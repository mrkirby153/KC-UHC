package me.mrkirby153.kcuhc.command;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;

public class CommandDiscord extends BaseCommand{
    private final String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to perform this command!");
            return true;
        }
        Player player = (Player) sender;
        if(args[0].equalsIgnoreCase("reconnect")){
            UHC.discordHandler.connect();
        }
        if(args[0].equalsIgnoreCase("join")){
            BaseComponent component = UtilChat.generateFormattedChat("To add the discord bot to your server, click ", ChatColor.GREEN);
            component.addExtra(UtilChat.generateHyperlink(UtilChat.generateBoldChat("[HERE]", ChatColor.BLUE),
                    "https://discordapp.com/oauth2/authorize?&client_id=169671604131856384&scope=bot&permissions=66202682", new BaseComponent[]{
                            UtilChat.generateFormattedChat("Click to add the Discord bot to your server!", ChatColor.WHITE)
                    }));
            player.spigot().sendMessage(component);
        }
        if(args[0].equalsIgnoreCase("serverlink")){
            String guildId = args[1];
            String serverId = UHC.plugin.getConfig().getString("discord.serverId");
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(serverId);
            out.writeUTF("link");
            out.writeUTF(serverId);
            out.writeUTF(guildId);
            UHC.discordHandler.sendMessage(out.toByteArray());
            ((Player) sender).spigot().sendMessage(UtilChat.generateFormattedChat("Linked this minecraft server to the discord server!", ChatColor.GREEN, 12));
        }
        if (args[0].equalsIgnoreCase("link")) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(UHC.plugin.serverId());
            out.writeUTF("linkCode");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(player.getName());
            ByteArrayDataInput response = UHC.discordHandler.sendMessage(out.toByteArray());
            int responseCode = response.readInt();
            String code = response.readUTF();
            player.sendMessage(ChatColor.GREEN + "Please log into handler and in the uhc-link channel, enter the following command: ");
            player.sendMessage(ChatColor.AQUA + "!uhcbot link " + code);
            return true;
        }
        if(restrictAdmin(sender))
            return true;
        if(args[0].equalsIgnoreCase("cinit")){
//            UHC.handler.initChannels();
            sender.sendMessage(UtilChat.generateFormattedChat("Generating discord channels", ChatColor.GOLD, 0).toLegacyText());
            UHC.discordHandler.createAllTeamChannels(()-> player.spigot().sendMessage(UtilChat.generateFormattedChat("Channels generated!", ChatColor.GREEN, 0)));
        }
        if(args[0].equalsIgnoreCase("shutdown")){
//            UHC.handler.shutdown();
            sender.sendMessage(UtilChat.generateFormattedChat("Deleting discord channels", ChatColor.GOLD, 0).toLegacyText());
            UHC.discordHandler.deleteAllTeamChannels(()->player.spigot().sendMessage(UtilChat.generateFormattedChat("Done!", ChatColor.GRAY, 0)));
        }
        if(args[0].equalsIgnoreCase("disperse")){
//            UHC.handler.sendEveryoneToChannels();
            player.spigot().sendMessage(UtilChat.generateFormattedChat("Sending everyone to their channels", ChatColor.GREEN, 0));
            UHC.arena.sendEveryoneToTeamChannels();
        }
        if(args[0].equalsIgnoreCase("bringtolobby")){
            UHC.arena.bringEveryoneToLobby();
            player.spigot().sendMessage(UtilChat.generateFormattedChat("Sending everyone to the lobby", ChatColor.GREEN, 4));
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
