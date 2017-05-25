package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.utils.UtilChat;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandUHCWorlds extends BaseCommand {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (restrictAdmin(sender))
            return true;
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("worlds")) {
                File[] files = Bukkit.getWorldContainer().listFiles(pathname -> pathname != null && pathname.isDirectory()
                        && pathname.getName().startsWith("UHC_") && !(pathname.getName().contains("nether") || pathname.getName().contains("end")));
                ArrayList<String> worlds = new ArrayList<>();
                if (files != null) {
                    worlds.addAll(Arrays.stream(files).map(File::getName).collect(Collectors.toList()));
                }
                StringBuilder worldList = new StringBuilder();
                for (String s : worlds) {
                    worldList.append(s).append(", ");
                }
                if (worldList.length() == 0) {
                    sender.sendMessage(UtilChat.message("No worlds!"));
                    return true;
                }
                worldList = new StringBuilder(worldList.substring(0, worldList.length() - 2));
                sender.sendMessage(UtilChat.message("Available worlds: " + ChatColor.GOLD + worldList));
                return true;
            }
            if (args[0].equalsIgnoreCase("create")) {
                sender.sendMessage(UtilChat.message("Generating world..."));
                String name = UHC.getInstance().multiWorldHandler.createWorld().getName();
                sender.sendMessage(UtilChat.message("Creation of " + ChatColor.GOLD + name + ChatColor.GRAY + " complete!"));
                return true;
            }

        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("tp")) {
                World w = Bukkit.getWorld(args[1]);
                if (w == null) {
                    sender.sendMessage(UtilChat.generateLegacyError("That world does not exist!"));
                    return true;
                }
                if (restrictPlayer(sender))
                    return true;
                ((Player) sender).teleport(w.getSpawnLocation());
                return true;
            }
            if (args[0].equalsIgnoreCase("set")) {
                String id = args[1];
                sender.sendMessage(UtilChat.message("Selecting world " + ChatColor.GOLD + id));
                if (!new File(Bukkit.getWorldContainer(), "UHC_" + id).exists()) {
                    sender.sendMessage(UtilChat.generateLegacyError("That world does not exist!"));
                    return true;
                }
                UHC.getInstance().multiWorldHandler.setWorld(id);
                sender.sendMessage(UtilChat.message("Selected world " + ChatColor.GOLD + id));
                return true;
            }
            if (args[0].equalsIgnoreCase("delete")) {
                String id = args[1];
                if (!new File(Bukkit.getWorldContainer(), "UHC_" + id).exists()) {
                    sender.sendMessage(UtilChat.generateLegacyError("That world does not exist!"));
                    return true;
                }
                UHC.getInstance().multiWorldHandler.deleteUHCWorld("UHC_" + id);
                sender.sendMessage(UtilChat.message("Deleted world " + ChatColor.GOLD + id));
                return true;
            }
        }
        return false;
    }
}
