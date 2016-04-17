package me.mrkirby153.kcuhc.command;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.noteBlock.JukeboxHandler;
import me.mrkirby153.kcuhc.noteBlock.NoteBlockSong;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

public class CommandMusic extends BaseCommand{


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(restrictPlayer(sender))
            return true;
        Player player = (Player) sender;
        if(args.length == 1){
            if(args[0].equalsIgnoreCase("on")){
                JukeboxHandler.startPlayingForUser(player);
                player.spigot().sendMessage(UtilChat.generateFormattedChat("You have enabled music again. Happy listening!", ChatColor.GREEN, 0));
                return true;
            }
            if(args[0].equalsIgnoreCase("off")){
                disableMusic(player);
            }
            if(restrictAdmin(sender))
                return true;
            if(args[0].equalsIgnoreCase("next")){
                JukeboxHandler.nextSong();
                return true;
            }
            if(args[0].equalsIgnoreCase("pause")){
                JukeboxHandler.pauseJukebox();
            }
            if(args[0].equalsIgnoreCase("stop")){
                JukeboxHandler.shutdown();
            }
            if(args[0].equalsIgnoreCase("resume")){
                JukeboxHandler.resumeJukebox();
            }
            if(args[0].equalsIgnoreCase("toggledebug")){
                NoteBlockSong.debug = !NoteBlockSong.debug;
                sender.sendMessage("Toggled noteblock song debugging");
                return true;
            }
            return true;
        }
        if(args.length == 2){
            if(restrictAdmin(sender))
                return true;
            if(args[0].equalsIgnoreCase("song")){
                String song = args[1];
                if(!song.endsWith(".nbsp"))
                    song = song + ".nbsp";

                File songs = new File(new File(UHC.plugin.getDataFolder(), "songs"), song);
                if(!songs.exists()){
                    player.spigot().sendMessage(UtilChat.generateError("That song does not exist!"));
                    return true;
                }
                JukeboxHandler.playSong(songs);
                return true;
            }
        }
        return false;
    }

    private void disableMusic(Player player){
        JukeboxHandler.stopPlayingForUser(player);
        player.spigot().sendMessage(UtilChat.generateFormattedChat("You have stopped listening to music, type /music on to resume", ChatColor.DARK_GREEN, 0));
        for(Player p : Bukkit.getOnlinePlayers()){
            if(p.getUniqueId() == player.getUniqueId())
                continue;
            p.spigot().sendMessage(UtilChat.generateFormattedChat(player.getName()+" is a bum and doesn't want to listen to the music!", ChatColor.GOLD, 8));
        }
    }
}
