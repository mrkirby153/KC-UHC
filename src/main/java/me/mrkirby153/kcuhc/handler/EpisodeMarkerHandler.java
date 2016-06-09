package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EpisodeMarkerHandler implements Runnable {


    private int episodeLength;

    private long startedOn;

    private long nextAnnounce;

    private boolean running = false;

    private int episodeNumber = 1;

    private SimpleDateFormat sdf = new SimpleDateFormat("YYY-MM-dd HH:mm:ss");


    public EpisodeMarkerHandler(UHC uhc) {
        if (uhc.getConfig().getBoolean("episodes.use")) {
            episodeLength = uhc.getConfig().getInt("episodes.duration");
        }
        uhc.getServer().getScheduler().scheduleSyncRepeatingTask(uhc, this, 0L, 1L);
    }


    public void startTracking() {
        this.episodeNumber = 1;
        this.startedOn = System.currentTimeMillis();
        this.nextAnnounce = System.currentTimeMillis() + (1000 * 60 * episodeLength);
        running = true;
        System.out.println("=== [Started episode tracking on " + sdf.format(new Date(startedOn)) + " ] ===");
        System.out.println("=== [Next episode (" + episodeNumber + ") in " + episodeLength + " minutes  (" + sdf.format(new Date(nextAnnounce)) + ") ] ===");
    }

    public void stopTracking() {
        System.out.println("=== [ STOPPED EPISODE TRACKING } ===");
        running = false;
    }

    @Override
    public void run() {
        if (System.currentTimeMillis() > nextAnnounce && running) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(ChatColor.BLUE + UtilTime.format(1, nextAnnounce - startedOn, UtilTime.TimeUnit.FIT) + " in! (End of Episode " + episodeNumber + ")");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F);
            }
            nextAnnounce = System.currentTimeMillis() + (1000 * 60 * episodeLength);
            System.out.println("=== [Next Episode (" + ++episodeNumber + ") in " + episodeLength + " minutes (" + sdf.format(new Date(nextAnnounce)) + ") ] ===");
        }
    }
}
