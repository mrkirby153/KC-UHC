package me.mrkirby153.kcuhc.module.msc;

import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTime;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EpisodeMarkerHandler extends UHCModule {


    private int episodeLength;

    private long startedOn;

    private long nextAnnounce;

    private boolean running = false;

    private int episodeNumber = 1;

    private SimpleDateFormat sdf = new SimpleDateFormat("YYY-MM-dd HH:mm:ss");


    public EpisodeMarkerHandler() {
        super(Material.FEATHER, 0, "Episode Marker", false, "Periodically broadcasts episode markers");
    }

    @Override
    public void onEnable() {
        episodeLength = getPlugin().getConfig().getInt("episodes.duration");
        Bukkit.broadcastMessage(UtilChat.message("Episode marks every " + episodeLength + " minutes"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if(event.getType() != UpdateType.FAST)
            return;
        if(!running)
            return;
        if (System.currentTimeMillis() > nextAnnounce) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(ChatColor.BLUE + UtilTime.format(1, nextAnnounce - startedOn, UtilTime.TimeUnit.FIT) + " in! (End of Episode " + episodeNumber + ")");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F);
            }
            nextAnnounce = System.currentTimeMillis() + (1000 * 60 * episodeLength);
            System.out.println("=== [Next Episode (" + ++episodeNumber + ") in " + episodeLength + " minutes (" + sdf.format(new Date(nextAnnounce)) + ") ] ===");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if(event.getTo() == UHCArena.State.RUNNING)
            startTracking();
        if(event.getTo() == UHCArena.State.ENDGAME)
            stopTracking();
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
}
