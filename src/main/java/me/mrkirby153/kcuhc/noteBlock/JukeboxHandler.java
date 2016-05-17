package me.mrkirby153.kcuhc.noteBlock;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class JukeboxHandler implements Listener {

    private static List<File> songs = new ArrayList<>();

    private static List<UUID> playersNotListening = new ArrayList<>();

    private static boolean playing = false;
    private static boolean paused = false;
    private static boolean started = false;

    private static NoteBlockSong currentSong;

    private static final Random random = new Random();

    private static BossBar nowPlayingBar;

    public static void initJukebox(File location) {
        if (location == null || location.listFiles() == null)
            return;
        songs = Arrays.asList(location.listFiles());
        nowPlayingBar = Bukkit.createBossBar(ChatColor.GOLD + "NOW PLAYING: " + ChatColor.GREEN + "???", BarColor.PINK, BarStyle.SOLID);
    }

    public static void startJukebox() {
        if (!started) {
            playing = true;
            started = true;
            nextSong();
        } else if (paused)
            resumeJukebox();
    }

    public static void pauseJukebox() {
        playing = false;
        paused = true;
        if (currentSong != null)
            currentSong.pause();
        nowPlayingBar.removeAll();
    }

    public static void playSong(File songPath) {
        if (currentSong != null)
            currentSong.stop(false);
        currentSong = new NoteBlockSong(songPath);
        play();
    }

    public static void resumeJukebox() {
        playing = true;
        paused = false;
        if (currentSong != null)
            currentSong.resume();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (playersNotListening.contains(p.getUniqueId()))
                continue;
            currentSong.addPlayer(p);
            nowPlayingBar.addPlayer(p);
        }
    }

    public static void nextSong() {
        File nextFile = songs.get(random.nextInt(songs.size()));
        if (!nextFile.getAbsolutePath().endsWith(".nbsp"))
            nextSong();
        else {
            if (currentSong != null)
                currentSong.stop(false);
            nowPlayingBar.removeAll();
            currentSong = new NoteBlockSong(nextFile);
            play();
        }
    }

    public static void stopPlayingForUser(Player player) {
        playersNotListening.add(player.getUniqueId());
        if (currentSong != null)
            currentSong.removePlayer(player);
        nowPlayingBar.removePlayer(player);
    }

    public static void startPlayingForUser(Player player) {
        if (!playersNotListening.contains(player.getUniqueId()))
            return;
        playersNotListening.remove(player.getUniqueId());
        if (currentSong != null)
            currentSong.addPlayer(player);
        nowPlayingBar.addPlayer(player);
    }

    private static void play() {
        nowPlayingBar.removeAll();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (playersNotListening.contains(p.getUniqueId()))
                continue;
            currentSong.addPlayer(p);
            nowPlayingBar.addPlayer(p);
        }
        currentSong.play();
        nowPlayingBar.setTitle(ChatColor.GOLD + "NOW PLAYING: " + ChatColor.GREEN + currentSong.getSongName());
    }

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent event) {
        if (UHC.arena.currentState() != UHCArena.State.RUNNING)
            if (currentSong != null) {
                if (!playersNotListening.contains(event.getPlayer().getUniqueId())) {
                    currentSong.addPlayer(event.getPlayer());
                }
                if (!nowPlayingBar.getPlayers().contains(event.getPlayer())) {
                    nowPlayingBar.addPlayer(event.getPlayer());
                }

            }
    }

    @EventHandler
    public void songEnd(SongEndedEvent event) {
        if (playing) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    System.out.println("Playing next song...");
                    nextSong();
                }
            }.runTaskLater(UHC.plugin, 40);
        }
    }

    public static void shutdown() {
        if (currentSong != null)
            currentSong.stop(false);
        nowPlayingBar.removeAll();
    }
}
