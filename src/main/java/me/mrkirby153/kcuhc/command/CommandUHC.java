package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import me.mrkirby153.kcuhc.noteBlock.JukeboxHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;

public class CommandUHC extends BaseCommand {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (restrictAdmin(sender))
                return true;
            if (args[0].equalsIgnoreCase("players")) {
                String playerNames = "";
                for (Player p : UHC.arena.players()) {
                    playerNames += p.getDisplayName() + ChatColor.RESET + ", ";
                }
                playerNames = playerNames.substring(0, playerNames.length() - 2);
                sender.sendMessage(ChatColor.GOLD + "Current players: " + ChatColor.RESET + playerNames);
                return true;
            }
            if (args[0].equalsIgnoreCase("start")) {
                UHC.arena.startCountdown();
                return true;
            }
            if (args[0].equalsIgnoreCase("stop")) {
                UHC.arena.stop("Nobody");
                return true;
            }
            if (args[0].equalsIgnoreCase("state")) {
                sender.sendMessage(UHC.arena.currentState().toString());
                return true;
            }
            if (args[0].equalsIgnoreCase("generate")) {
                JukeboxHandler.shutdown();
                UHC.arena.generate();
                return true;
            }
            if (args[0].equalsIgnoreCase("init")) {
                UHC.arena.initialize();
                return true;
            }
            if (args[0].equalsIgnoreCase("disable")) {
                UHC.arena.essentiallyDisable();
                Bukkit.broadcastMessage("Disabling UHC plugin!");
                return true;
            }
            if (args[0].equalsIgnoreCase("toggleending")) {
                UHC.arena.toggleShouldEndCheck();
                return true;
            }
            if (args[0].equalsIgnoreCase("singlepersonteams")) {
                if(UHC.discordHandler != null)
                    UHC.discordHandler.shutdown();
                List<ChatColor> usedColors = new ArrayList<>();
                List<ChatColor> blacklistedColors = Arrays.asList(ChatColor.BOLD, ChatColor.STRIKETHROUGH, ChatColor.RESET, ChatColor.MAGIC, ChatColor.UNDERLINE);
                HashMap<Player, UHCTeam> teams = new HashMap<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (TeamHandler.isSpectator(p)) {
                        continue;
                    }
                    ChatColor[] colors = ChatColor.values();
                    Random r = new Random();
                    ChatColor chosenColor = colors[r.nextInt(colors.length)];
                    while (usedColors.contains(chosenColor) && !blacklistedColors.contains(chosenColor))
                        chosenColor = colors[r.nextInt(colors.length)];
                    usedColors.add(chosenColor);
                    sender.sendMessage(chosenColor + "Created team " + p.getName());
                    UHC.arena.newTeam(p.getName(), chosenColor);
                    teams.put(p, TeamHandler.getTeamByName(p.getName()));
                }
                for(Map.Entry<Player, UHCTeam> e : teams.entrySet()){
                    TeamHandler.joinTeam(e.getValue(), e.getKey());
                }
                sender.sendMessage(UtilChat.generateFormattedChat("Created teams and assigned teams!", ChatColor.GREEN, 0).toLegacyText());
                return true;
            }
            if (args[0].equalsIgnoreCase("deathmatch") || args[0].equalsIgnoreCase("dm")) {
                if (UHC.arena.currentState() != UHCArena.State.RUNNING) {
                    sender.sendMessage(UtilChat.generateLegacyError("You cannot start a deathmatch if the game isn't running!"));
                    return true;
                }
                if (UHC.arena.getWorld().getWorldBorder().getSize() != UHC.arena.endSize()) {
                    sender.sendMessage(UtilChat.generateLegacyError("The worldborder must finish its travel before you can being DM!"));
                    return true;
                }
                UHC.arena.startDeathmatch();
                sender.sendMessage("Deathmatch started!");
                return true;
            }
        }
        if (args.length == 2) {
            if (restrictAdmin(sender)) {
                return true;
            }
            if (args[0].equalsIgnoreCase("state")) {
                try {
                    UHCArena.State s = UHCArena.State.valueOf(args[1].toUpperCase());
                    UHC.arena.setState(s);
                    sender.sendMessage("Set state to " + s.toString());
                    return true;
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("Invalid state!"));
                    return true;
                }
            }
            if(args[0].equalsIgnoreCase("endgamestate")){
                try {
                    UHCArena.EndgamePhase p = UHCArena.EndgamePhase.valueOf(args[1].toUpperCase());
                    UHC.arena.setEndgamePhase(p);
                    sender.sendMessage("Set state to " + p.toString());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("Invalid state!"));
                    return true;
                }
            }
            if(args[0].equalsIgnoreCase("endgametime")){
                long newTime = Long.parseLong(args[1]) + System.currentTimeMillis();
                try{
                    Field f = UHCArena.class.getDeclaredField("nextEndgamePhaseIn");
                    f.setAccessible(true);
                    f.set(UHC.arena, newTime);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (args[0].equalsIgnoreCase("winner")) {
                try {
                    Field f = Color.class.getDeclaredField(args[1].toUpperCase());
                    UHC.arena.temp_FireworkLaunch((Color) f.get(null));
                    return true;
                } catch (NoSuchFieldException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("Invalid color!"));
                    return true;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        //      /uhc create x z world size endSize duration
        if (args.length == 7) {
            if (args[0].equalsIgnoreCase("create")) {
                if (restrictAdmin(sender))
                    return true;
                if (!isNumber(args[1])) {
                    sender.sendMessage(UtilChat.generateError("X coord must be a number").toLegacyText());
                    return true;
                }
                if (!isNumber(args[2])) {
                    sender.sendMessage(UtilChat.generateError("Z coord must be a number").toLegacyText());
                    return true;
                }
                int x = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                World w = Bukkit.getWorld(args[3]);
                int size = Integer.parseInt(args[4]);
                int endSize = Integer.parseInt(args[5]);
                int duration = Integer.parseInt(args[6]);
                UHC.arena = new UHCArena(w, size, endSize, duration, new Location(w, x, 0, z));
                UHC.arena.saveToFile();
                sender.sendMessage(UtilChat.generateFormattedChat("Created arena successfully!", ChatColor.GREEN, 0).toLegacyText());
                return true;
            }
        }
        return false;
    }
}
