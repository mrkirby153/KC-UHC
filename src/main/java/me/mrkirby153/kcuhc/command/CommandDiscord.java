package me.mrkirby153.kcuhc.command;

import com.google.common.io.ByteArrayDataInput;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.discord.commands.Link;
import me.mrkirby153.kcuhc.discord.commands.LinkCode;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
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
            ByteArrayDataInput response = new LinkCode(player).send();
            int responseCode = response.readInt();
            String code = response.readUTF();
            BaseComponent line = UtilChat.generateFormattedChat("=============================================", ChatColor.GREEN, 10);
            BaseComponent padding = new TextComponent(" ");
            BaseComponent info = UtilChat.generateFormattedChat("   Your link code is ", ChatColor.WHITE, 0);
            info.addExtra(UtilChat.generateFormattedChat(code, ChatColor.BLUE));
            BaseComponent onServer = UtilChat.generateFormattedChat("   On the discord server, type ", ChatColor.WHITE, 0);
            String codeMsg = "!uhcbot link " + code;
            BaseComponent component = UtilChat.generateFormattedChat(codeMsg, ChatColor.BLUE, 0);
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{UtilChat.generateFormattedChat("Click here to put your code in your chat window for easy copying", ChatColor.WHITE, 0)}));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, codeMsg));
            onServer.addExtra(component);
            BaseComponent click = UtilChat.generateFormattedChat("   Click the highlighted part of the above message to copy", ChatColor.WHITE, 0);
            BaseComponent click2 = UtilChat.generateFormattedChat("   your code", ChatColor.WHITE, 0);
            UtilChat.sendMultiple(player, line, padding, padding, info, onServer, click, click2, padding, padding, line);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1F, 1F);
            return true;
        }
        if (restrictAdmin(sender)) {
            return true;
        }
        if (args[0].equalsIgnoreCase("invite")) {
            for(Player p : Bukkit.getOnlinePlayers()) {
                BaseComponent line = UtilChat.generateFormattedChat("=============================================", ChatColor.GREEN, 10);
                BaseComponent padding = new TextComponent(" ");
                BaseComponent message = UtilChat.generateFormattedChat("             >>> Click here to join the discord server <<<", ChatColor.GRAY, 0);
                message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, UHC.plugin.getConfig().getString("discord.inviteUrl")));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{UtilChat.generateFormattedChat("Join the discord server", ChatColor.WHITE, 0)}));
                UtilChat.sendMultiple(p, line, padding, padding, padding, padding, message, padding, padding, padding, line);
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1F, 0.8F);
            }
        }
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
            new Link(serverId, guildId).send();
            sender.sendMessage(UtilChat.message("Linked this server to the discord server"));
        }
        if (args[0].equalsIgnoreCase("cinit")) {
            sender.sendMessage(UtilChat.message("Generating discord channels..."));
            UHC.discordHandler.createAllTeamChannels(() -> sender.sendMessage(UtilChat.message("Discord channels generated!")));
        }
        if (args[0].equalsIgnoreCase("shutdown")) {
            sender.sendMessage(UtilChat.message("Removing discord channels..."));
            UHC.discordHandler.deleteAllTeamChannels(() -> sender.sendMessage(UtilChat.message("Done!")));
        }
        if (args[0].equalsIgnoreCase("disperse")) {
            sender.sendMessage(UtilChat.message("Sending everyone to their channels..."));
            UHC.arena.sendEveryoneToTeamChannels();
        }
        if (args[0].equalsIgnoreCase("bringtolobby")) {
            UHC.arena.bringEveryoneToLobby();
            sender.sendMessage(UtilChat.message("Bringing everyone to the lobby..."));
        }
        if (args[0].equalsIgnoreCase("linked")) {
            player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Looking up all online player's discord link status");
            UHC.discordHandler.getAllLinkedPlayers(data -> data.entrySet().forEach(e -> {
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
            }));
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
