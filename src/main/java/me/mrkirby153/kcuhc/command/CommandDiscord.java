package me.mrkirby153.kcuhc.command;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;

public class CommandDiscord extends BaseCommand {
    private final String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to perform this command!");
            return true;
        }
        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("link")) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(UHC.plugin.serverId());
            out.writeUTF("linkCode");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(player.getName());
            ByteArrayDataInput response = UHC.discordHandler.sendMessage(out.toByteArray());
            int responseCode = response.readInt();
            String code = response.readUTF();
            BaseComponent line = UtilChat.generateFormattedChat("=============================================", ChatColor.GREEN, 10);
            BaseComponent padding = new TextComponent(" ");
            BaseComponent info = UtilChat.generateFormattedChat("   Your link code is ", ChatColor.WHITE, 0);
            info.addExtra(UtilChat.generateFormattedChat(code, ChatColor.BLUE));
            BaseComponent onServer = UtilChat.generateFormattedChat("   On the discord server, type ", ChatColor.WHITE, 0);
            onServer.addExtra(UtilChat.generateFormattedChat("!uhcbot link " + code, ChatColor.BLUE, 0));
            UtilChat.sendMultiple(player, line, padding, padding, padding, info, onServer, padding, padding, padding, line);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1F, 1F);
            return true;
        }
        if (restrictAdmin(sender))
            return true;
        if (args[0].equalsIgnoreCase("reconnect")) {
            UHC.discordHandler.connect();
        }
        if (args[0].equalsIgnoreCase("join")) {
            BaseComponent component = UtilChat.generateFormattedChat("To add the discord bot to your server, click ", ChatColor.GREEN);
            component.addExtra(UtilChat.generateHyperlink(UtilChat.generateBoldChat("[HERE]", ChatColor.BLUE),
                    "https://discordapp.com/oauth2/authorize?&client_id=169671604131856384&scope=bot&permissions=66202682", new BaseComponent[]{
                            UtilChat.generateFormattedChat("Click to add the Discord bot to your server!", ChatColor.WHITE)
                    }));
            player.spigot().sendMessage(component);
        }
        if (args[0].equalsIgnoreCase("serverlink")) {
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
        if (args[0].equalsIgnoreCase("cinit")) {
//            UHC.handler.initChannels();
            sender.sendMessage(UtilChat.generateFormattedChat("Generating discord channels", ChatColor.GOLD, 0).toLegacyText());
            UHC.discordHandler.createAllTeamChannels(() -> player.spigot().sendMessage(UtilChat.generateFormattedChat("Channels generated!", ChatColor.GREEN, 0)));
        }
        if (args[0].equalsIgnoreCase("shutdown")) {
//            UHC.handler.shutdown();
            sender.sendMessage(UtilChat.generateFormattedChat("Deleting discord channels", ChatColor.GOLD, 0).toLegacyText());
            UHC.discordHandler.deleteAllTeamChannels(() -> player.spigot().sendMessage(UtilChat.generateFormattedChat("Done!", ChatColor.GRAY, 0)));
        }
        if (args[0].equalsIgnoreCase("disperse")) {
//            UHC.handler.sendEveryoneToChannels();
            player.spigot().sendMessage(UtilChat.generateFormattedChat("Sending everyone to their channels", ChatColor.GREEN, 0));
            UHC.arena.sendEveryoneToTeamChannels();
        }
        if (args[0].equalsIgnoreCase("bringtolobby")) {
            UHC.arena.bringEveryoneToLobby();
            player.spigot().sendMessage(UtilChat.generateFormattedChat("Sending everyone to the lobby", ChatColor.GREEN, 4));
        }
        if (args[0].equalsIgnoreCase("linked")) {
            player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Looking up all online player's discord link status");
            UHC.discordHandler.getAllLinkedPlayers(data -> {
                data.entrySet().forEach(e -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(e.getKey());
                    if (offlinePlayer == null)
                        return;
                    String value = e.getValue();
                    player.sendMessage(ChatColor.WHITE + offlinePlayer.getName());
                    if (value == null) {
                        player.sendMessage("  + " + ChatColor.RED + ChatColor.BOLD + "Unlinked!");
                    } else {
                        String discordName = value.split("::")[1].replace("\\:\\:", "::");
                        String discordId = value.split("::")[0].replace("\\:\\:", "::");
                        player.sendMessage("  + " + ChatColor.DARK_GREEN + discordName + " (" + discordId + ")");
                    }
                });
            });
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
