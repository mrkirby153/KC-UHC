package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.uhc.bot.network.PlayerInfo;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandLink;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandNewTeam;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandRemoveTeam;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;

public class CommandDiscord extends BaseCommand {
    private final String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private TeamHandler teamHandler;
    private UHC plugin;

    public CommandDiscord(TeamHandler teamHandler, UHC plugin){
        this.teamHandler = teamHandler;
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to perform this command!");
            return true;
        }
        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("link")) {
            String code = generateCode(5);
            setCode(player, code);
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
        if (args[0].equalsIgnoreCase("forcelink")) {
            String username = args[1];
            String discordId = args[2];
            Player p = Bukkit.getPlayer(username);
            if(p == null){
                player.sendMessage(UtilChat.generateLegacyError("That user was not found"));
            }
            PlayerInfo info = new PlayerInfo(p.getUniqueId(), p.getName());
            info.setDiscordUser(discordId);
            info.setLinked(true);
            UHC.uhcNetwork.getDatastore().addElement(info);
            p.sendMessage(UtilChat.message("Your account has been linked to discord by "+sender.getName()));
            player.sendMessage(UtilChat.message("Linked user " + username + " to the discord id " + discordId));
        }
        if (args[0].equalsIgnoreCase("invite")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                BaseComponent line = UtilChat.generateFormattedChat("=============================================", ChatColor.GREEN, 10);
                BaseComponent padding = new TextComponent(" ");
                BaseComponent message = UtilChat.generateFormattedChat("             >>> Click here to join the discord server <<<", ChatColor.GRAY, 0);
                message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, plugin.getConfig().getString("discord.inviteUrl")));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{UtilChat.generateFormattedChat("Join the discord server", ChatColor.WHITE, 0)}));
                UtilChat.sendMultiple(p, line, padding, padding, padding, padding, message, padding, padding, padding, line);
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1F, 0.8F);
            }
        }
        if (args[0].equalsIgnoreCase("join")) {
            BaseComponent component = UtilChat.generateFormattedChat("To add the discord bot to your server, click ", ChatColor.GREEN);
            component.addExtra(UtilChat.generateHyperlink(UtilChat.generateBoldChat("[HERE]", ChatColor.BLUE),
                    "https://discordapp.com/oauth2/authorize?&client_id=169671604131856384&scope=bot&permissions=285223952", new BaseComponent[]{
                            UtilChat.generateFormattedChat("Click to add the Discord bot to your server!", ChatColor.WHITE)
                    }));
            player.spigot().sendMessage(component);
        }
        if (args[0].equalsIgnoreCase("serverlink")) {
            String guildId = args[1];
            String serverId = plugin.getConfig().getString("discord.serverId");
            new BotCommandLink(serverId, guildId).publishBlocking();
            sender.sendMessage(UtilChat.message("Linked this server to the discord server"));
        }
        if (args[0].equalsIgnoreCase("cinit")) {
            sender.sendMessage(UtilChat.message("Generating discord channels..."));
            teamHandler.teams().forEach(t -> {
                new BotCommandNewTeam(plugin.serverId(), t.getTeamName()).publishBlocking();
                System.out.println("Created channel " + t.getTeamName());
            });
            UtilChat.message("Discord channels generated!");
        }
        if (args[0].equalsIgnoreCase("shutdown")) {
            sender.sendMessage(UtilChat.message("Removing discord channels..."));
            teamHandler.teams().forEach(t -> new BotCommandRemoveTeam(plugin.serverId(), t.getTeamName()).publishBlocking());
            sender.sendMessage("Done!");
        }
        if (args[0].equalsIgnoreCase("disperse")) {
            sender.sendMessage(UtilChat.message("Sending everyone to their channels..."));
            plugin.arena.sendEveryoneToTeamChannels();
        }
        if (args[0].equalsIgnoreCase("bringtolobby")) {
            plugin.arena.bringEveryoneToLobby();
            sender.sendMessage(UtilChat.message("Bringing everyone to the lobby..."));
        }
        if (args[0].equalsIgnoreCase("linked")) {
            player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Looking up all online player's discord link status");
            Bukkit.getOnlinePlayers().forEach(p -> {
                PlayerInfo info = UHC.uhcNetwork.getPlayerInfo(p.getUniqueId());
                if (info == null || !info.isLinked()) {
                    sender.sendMessage(ChatColor.WHITE + p.getName() + " is not linked!");
                    return;
                }
                sender.sendMessage(ChatColor.GREEN + p.getName() + " is linked!");
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

    private void setCode(Player player, String code) {
        PlayerInfo info = UHC.uhcNetwork.getPlayerInfo(player.getUniqueId());
        if (info == null) {
            info = new PlayerInfo(player.getUniqueId(), player.getName());
            UHC.uhcNetwork.getDatastore().addElement(info);
            setCode(player, code);
            return;
        }
        info.setLinkCode(code);
        info.update();
    }
}
