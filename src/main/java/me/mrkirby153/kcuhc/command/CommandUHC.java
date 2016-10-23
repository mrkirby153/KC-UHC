package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.gui.admin.GameAdminInventory;
import me.mrkirby153.kcuhc.handler.FreezeHandler;
import me.mrkirby153.kcuhc.handler.GameListener;
import me.mrkirby153.kcuhc.handler.MOTDHandler;
import me.mrkirby153.kcuhc.handler.RegenTicket;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTime;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;

public class CommandUHC extends BaseCommand {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player)
                new GameAdminInventory(UHC.plugin, (Player) sender);
            return true;
        }
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
            if (args[0].equalsIgnoreCase("whitelistTeams")) {
                Bukkit.getServer().setWhitelist(true);
                for (UHCTeam t : TeamHandler.teams()) {
                    for (UUID u : t.getPlayers()) {
                        Bukkit.getServer().getOfflinePlayer(u).setWhitelisted(true);
                    }
                }
                sender.sendMessage(UtilChat.message("Whitelisted all team members!"));
                return true;
            }
            if (args[0].equalsIgnoreCase("start")) {
                sender.sendMessage(UtilChat.message("Started countdown"));
                UHC.arena.startCountdown();
                return true;
            }
            if (args[0].equalsIgnoreCase("stop")) {
                sender.sendMessage(UtilChat.message("Stopped game"));
                UHC.arena.stop("Nobody");
                return true;
            }
            if (args[0].equalsIgnoreCase("state")) {
                sender.sendMessage(UtilChat.message("UHC is in state: " + ChatColor.GOLD + UHC.arena.currentState().toString()));
                return true;
            }
            if (args[0].equalsIgnoreCase("generate")) {
                sender.sendMessage(UtilChat.message("Started generation of map"));
                UHC.arena.generate();
                return true;
            }
            if (args[0].equalsIgnoreCase("init")) {
                UHC.arena.initialize();
                return true;
            }
            if (args[0].equalsIgnoreCase("disable")) {
                UHC.arena.essentiallyDisable();
                Bukkit.broadcastMessage(UtilChat.message("Disabling UHC plugin!"));
                return true;
            }
            if (args[0].equalsIgnoreCase("debugStart")) {
                if (UHC.arena.getProperties().CHECK_ENDING.get())
                    UHC.arena.toggleShouldEndCheck();
                if (UHC.arena.getProperties().SPREAD_PLAYERS.get())
                    UHC.arena.toggleSpreadingPlayers();
                if (TeamHandler.getTeamByName("debug") == null)
                    UHC.arena.newTeam("debug", ChatColor.GOLD);
                for (Player p : Bukkit.getOnlinePlayers())
                    TeamHandler.joinTeam(TeamHandler.getTeamByName("debug"), p);
                Bukkit.getScheduler().runTaskLater(UHC.plugin, () -> {
                    sender.setOp(true);
                    sender.sendMessage(UtilChat.message("You are now op"));
                }, 220);
                UHC.arena.startCountdown();
                sender.sendMessage(UtilChat.message("Debug starting"));
                return true;
            }
            if (args[0].equalsIgnoreCase("toggleending")) {
                UHC.arena.toggleShouldEndCheck();
                return true;
            }
            if (args[0].equalsIgnoreCase("togglespread")) {
                UHC.arena.toggleSpreadingPlayers();
                return true;
            }
            if (args[0].equalsIgnoreCase("freeze")) {
                UHC.arena.freeze();
                return true;
            }
            if (args[0].equalsIgnoreCase("unfreeze")) {
                UHC.arena.unfreeze();
                return true;
            }
            if (args[0].equalsIgnoreCase("freezebypass")) {
                Player player = (Player) sender;
                FreezeHandler.freezebypass(player);
                return true;
            }
            if (args[0].equalsIgnoreCase("singlepersonteams")) {
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
                    sender.sendMessage(UtilChat.message(chosenColor + "Created team " + p.getName()));
                    UHC.arena.newTeam(p.getName(), chosenColor);
                    teams.put(p, TeamHandler.getTeamByName(p.getName()));
                    TeamHandler.leaveTeam(p);
                }
                for (Map.Entry<Player, UHCTeam> e : teams.entrySet()) {
                    TeamHandler.joinTeam(e.getValue(), e.getKey());
                }
                sender.sendMessage(UtilChat.message("Created and assigned teams"));
                return true;
            }
        }
        if (args.length == 2) {
            if (restrictAdmin(sender)) {
                return true;
            }
            if (args[0].equalsIgnoreCase("respawn")) {
                String playerName = args[1];
                Player player = Bukkit.getPlayer(playerName);
                if (player == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                    return true;
                }
                if (!GameListener.isDead(player)) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player is not dead!"));
                    return true;
                }
                if (!GameListener.validLocation(player)) {
                    sender.sendMessage(UtilChat.generateLegacyError("It is unsafe to spawn the player where they died! Use " + ChatColor.GOLD + "/uhc respawnhere " + args[1] + ChatColor.GRAY + " to spawn the player at your current location"));
                    return true;
                }
                GameListener.restorePlayerData(player, true);
                player.sendMessage(UtilChat.message("You have been respawned by " + ChatColor.GOLD + sender.getName()));
                player.setHealth(player.getMaxHealth() * 0.5);
                sender.sendMessage(UtilChat.message("Respawned " + ChatColor.GOLD + sender.getName()));
                return true;
            }
            if (args[0].equalsIgnoreCase("respawnhere")) {
                if (restrictPlayer(sender))
                    return true;
                String playerName = args[1];
                Player player = Bukkit.getPlayer(playerName);
                if (player == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                    return true;
                }
                if (!GameListener.isDead(player)) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player is not dead!"));
                    return true;
                }
                GameListener.restorePlayerData(player, false);
                player.teleport((Player) sender);
                player.setHealth(player.getMaxHealth() * 0.5);
                player.sendMessage(UtilChat.message("You have been respawned by " + ChatColor.GOLD + sender.getName()));
                sender.sendMessage(UtilChat.message("Respawned " + ChatColor.GOLD + sender.getName()));
                return true;
            }
            if (args[0].equalsIgnoreCase("state")) {
                try {
                    UHCArena.State s = UHCArena.State.valueOf(args[1].toUpperCase());
                    UHC.arena.setState(s);
                    sender.sendMessage(UtilChat.message("Set state to " + ChatColor.GOLD + s.toString()));
                    return true;
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("Invalid state!"));
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("endgamestate")) {
                try {
                    UHCArena.EndgamePhase p = UHCArena.EndgamePhase.valueOf(args[1].toUpperCase());
                    UHC.arena.setEndgamePhase(p);
                    sender.sendMessage(UtilChat.message("Set endgame state to " + ChatColor.GOLD + p.toString()));
                    return true;
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("Invalid state!"));
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("endgametime")) {
                long newTime = Long.parseLong(args[1]) + System.currentTimeMillis();
                try {
                    Field f = UHCArena.class.getDeclaredField("nextEndgamePhaseIn");
                    f.setAccessible(true);
                    f.set(UHC.arena, newTime);
                    sender.sendMessage(UtilChat.message("Next endgame phase in " + ChatColor.GOLD + UtilTime.format(1, Long.parseLong(args[1]), UtilTime.TimeUnit.FIT)));
                    return true;
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("Could not set the endgame time!"));
                }
            }
            if (args[0].equalsIgnoreCase("winner")) {
                try {
                    Field f = Color.class.getDeclaredField(args[1].toUpperCase());
                    UHC.arena.temp_FireworkLaunch((Color) f.get(null));
                    sender.sendMessage(UtilChat.message("Launching fireworks with color " + ChatColor.GOLD + args[1].toUpperCase()));
                    return true;
                } catch (NoSuchFieldException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("Invalid color!"));
                    return true;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (args[0].equalsIgnoreCase("unfreeze")) {
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                    return true;
                }
                FreezeHandler.playerUnfreeze(p);
            }
            if (args[0].equalsIgnoreCase("freeze")) {
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                    return true;
                }
                FreezeHandler.freezePlayer(p);
            }
            if (args[0].equalsIgnoreCase("addHeartRow")) {
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                }
                sender.sendMessage(UtilChat.message("Added heart row to " + ChatColor.GOLD + args[1]));
                UHC.extraHealthHelper.addHeartRow(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("removeHeartRow")) {
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                }
                sender.sendMessage(UtilChat.message("Removed heart row from " + ChatColor.GOLD + args[1]));
                UHC.extraHealthHelper.removeHealthRow(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("rticket")) {
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                }
                RegenTicket.give(p);
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("preset")) {
                if (args[1].equalsIgnoreCase("load")) {
                    String propName = args[2];
                    if (!ArenaProperties.propertyExists(propName)) {
                        sender.sendMessage(UtilChat.generateLegacyError("That preset does not exist!"));
                        return true;
                    }
                    UHC.arena.setProperties(ArenaProperties.loadProperties(propName));
                    sender.sendMessage(UtilChat.message("Loaded property file " + ChatColor.GOLD + propName + ".json"));
                    return true;
                }
                if(args[1].equalsIgnoreCase("save")){
                    String propName = args[2];
                    ArenaProperties.saveProperties(UHC.arena.getProperties(), propName);
                    sender.sendMessage(UtilChat.message("Saved property file " + ChatColor.GOLD + propName + ".json"));
                    return true;
                }
            }
        }
        //      /uhc create x z world size endSize duration
        if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("closeServer")) {
                String msg = "";
                for (int i = 1; i < args.length; i++) {
                    msg += args[i] + " ";
                }
                Bukkit.getWhitelistedPlayers().forEach(p -> p.setWhitelisted(false));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!UHC.isAdmin(p)) {
                        p.kickPlayer(ChatColor.GOLD + "Kicked by " + sender.getName() + "\n\n" + ChatColor.RESET + msg.trim());
                    } else {
                        p.setWhitelisted(true);
                    }
                }
                MOTDHandler.setMotd(ChatColor.RED + "(Admin Only) " + ChatColor.GOLD + msg);
                Bukkit.setWhitelist(true);
                sender.sendMessage(UtilChat.message("Kicked all non-admins and enabled whitelist with " + ChatColor.GOLD + msg.trim()));
                return true;
            }
        }
        return false;
    }
}
