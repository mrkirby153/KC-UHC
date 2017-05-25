package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.gui.admin.GameAdminInventory;
import me.mrkirby153.kcuhc.handler.MOTDHandler;
import me.mrkirby153.kcuhc.handler.listener.GameListener;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.endgame.EndgameScenarioModule;
import me.mrkirby153.kcuhc.module.msc.RegenTicketModule;
import me.mrkirby153.kcuhc.module.player.SpreadPlayersModule;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.world.WorldStatus;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

public class CommandUHC extends BaseCommand {

    private TeamHandler teamHandler;
    private UHC plugin;

    public CommandUHC(UHC plugin, TeamHandler teamHandler) {
        this.teamHandler = teamHandler;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (UHC.isAdmin(sender.getName()) || sender.isOp())
                if (sender instanceof Player)
                    new GameAdminInventory(plugin, (Player) sender);
            return true;
        }
        if (args.length == 1) {
            if (restrictAdmin(sender))
                return true;
            if (args[0].equalsIgnoreCase("players")) {
                String playerNames = "";
                for (Player p : plugin.arena.players()) {
                    playerNames += p.getDisplayName() + ChatColor.RESET + ", ";
                }
                playerNames = playerNames.substring(0, playerNames.length() - 2);
                sender.sendMessage(ChatColor.GOLD + "Current players: " + ChatColor.RESET + playerNames);
                return true;
            }
            if (args[0].equalsIgnoreCase("location")) {
                if (restrictPlayer(sender))
                    return true;
                Player player = (Player) sender;
                Location l = player.getLocation();
                player.sendMessage(UtilChat.message(String.format("You are at " + ChatColor.GOLD + "%.2f, %.2f, %.2f " + ChatColor.GRAY + " in world " + ChatColor.GOLD + "%s", l.getX(), l.getY(), l.getZ(), l.getWorld().getName())));
                return true;
            }
            if (args[0].equalsIgnoreCase("whitelistTeams")) {
                Bukkit.getServer().setWhitelist(true);
                for (UHCTeam t : teamHandler.teams()) {
                    for (UUID u : t.getPlayers()) {
                        Bukkit.getServer().getOfflinePlayer(u).setWhitelisted(true);
                    }
                }
                sender.sendMessage(UtilChat.message("Whitelisted all team members!"));
                return true;
            }
            if (args[0].equalsIgnoreCase("start")) {
                if (plugin.arena.currentState() == UHCArena.State.GENERATING_WORLD) {
                    sender.sendMessage(UtilChat.generateLegacyError("The world is still generating! Wait until it finishes"));
                    return true;
                }
                WorldStatus status = UHC.getInstance().multiWorldHandler.getStatus(UHC.getInstance().uhcWorld);
                if (status != WorldStatus.PREGENERATED) {
                    sender.sendMessage(UtilChat.generateLegacyError("The world is not pregenerated! Please pregenerate it first"));
                    return true;
                }
                sender.sendMessage(UtilChat.message("Started countdown"));
                plugin.arena.startCountdown();
                return true;
            }
            if (args[0].equalsIgnoreCase("stop")) {
                sender.sendMessage(UtilChat.message("Stopped game"));
                plugin.arena.stop("Nobody", Color.WHITE);
                return true;
            }
            if (args[0].equalsIgnoreCase("state")) {
                sender.sendMessage(UtilChat.message("UHC is in state: " + ChatColor.GOLD + plugin.arena.currentState().toString()));
                return true;
            }
            if (args[0].equalsIgnoreCase("generate")) {
                sender.sendMessage(UtilChat.message("Started generation of map"));
                plugin.arena.generate();
                return true;
            }
            if (args[0].equalsIgnoreCase("init")) {
                plugin.arena.initialize();
                return true;
            }
            if (args[0].equalsIgnoreCase("debugStart")) {
                ModuleRegistry.getLoadedModule(EndgameScenarioModule.class).ifPresent(ModuleRegistry::unloadModule);
                ModuleRegistry.getLoadedModule(SpreadPlayersModule.class).ifPresent(ModuleRegistry::unloadModule);
                if (teamHandler.getTeamByName("debug") == null)
                    teamHandler.registerTeam(new UHCPlayerTeam("Debug", ChatColor.GOLD));
                for (Player p : Bukkit.getOnlinePlayers())
                    teamHandler.joinTeam(teamHandler.getTeamByName("debug"), p);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    sender.setOp(true);
                    sender.sendMessage(UtilChat.message("You are now op"));
                }, 220);
                plugin.arena.startCountdown();
                sender.sendMessage(UtilChat.message("Debug starting"));
                return true;
            }
            if (args[0].equalsIgnoreCase("spread")) {
                Optional<SpreadPlayersModule> module = ModuleRegistry.getLoadedModule(SpreadPlayersModule.class);
                if (!module.isPresent()) {
                    // Load the spread players module
                    Optional<SpreadPlayersModule> toLoad = ModuleRegistry.getModule(SpreadPlayersModule.class);
                    if (!toLoad.isPresent()) {
                        sender.sendMessage(UtilChat.generateLegacyError("Could not find module " + SpreadPlayersModule.class));
                        return true;
                    }
                    ModuleRegistry.loadModule(toLoad.get());
                    module = ModuleRegistry.getLoadedModule(SpreadPlayersModule.class);
                    if (!module.isPresent()) {
                        sender.sendMessage(UtilChat.generateLegacyError("Could not load the spread players module!"));
                        return true;
                    }
                    module.get().distributeTeams();
                    ModuleRegistry.unloadModule(module.get());
                } else {
                    module.get().distributeTeams();
                }
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
                    plugin.arena.setState(s);
                    sender.sendMessage(UtilChat.message("Set state to " + ChatColor.GOLD + s.toString()));
                    return true;
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("Invalid state!"));
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("winner")) {
                try {
                    Field f = Color.class.getDeclaredField(args[1].toUpperCase());
                    plugin.arena.temp_FireworkLaunch((Color) f.get(null));
                    sender.sendMessage(UtilChat.message("Launching fireworks with color " + ChatColor.GOLD + args[1].toUpperCase()));
                    return true;
                } catch (NoSuchFieldException e) {
                    sender.sendMessage(UtilChat.generateLegacyError("Invalid color!"));
                    return true;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (args[0].equalsIgnoreCase("addHeartRow")) {
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                }
                sender.sendMessage(UtilChat.message("Added heart row to " + ChatColor.GOLD + args[1]));
                plugin.extraHealthHelper.addHeartRow(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("removeHeartRow")) {
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                }
                sender.sendMessage(UtilChat.message("Removed heart row from " + ChatColor.GOLD + args[1]));
                plugin.extraHealthHelper.removeHealthRow(p);
                return true;
            }
            if (args[0].equalsIgnoreCase("rticket")) {
                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That player does not exist!"));
                    return true;
                }
                if (!ModuleRegistry.isLoaded(RegenTicketModule.class)) {
                    sender.sendMessage(UtilChat.generateLegacyError("Regen tickets aren't enabled!"));
                    return true;
                }
                ModuleRegistry.getLoadedModule(RegenTicketModule.class).ifPresent(m -> m.give(p));
                sender.sendMessage(UtilChat.message("Given " + ChatColor.GOLD + p.getName() + ChatColor.GRAY + " a regen ticket"));
                return true;
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
                    plugin.arena.setProperties(ArenaProperties.loadProperties(propName));
                    sender.sendMessage(UtilChat.message("Loaded property file " + ChatColor.GOLD + propName + ".json"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("save")) {
                    String propName = args[2];
                    ArenaProperties.saveProperties(plugin.arena.getProperties(), propName);
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
