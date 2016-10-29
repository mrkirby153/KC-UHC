package me.mrkirby153.kcuhc.arena;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.handler.BosssBarHandler;
import me.mrkirby153.kcuhc.utils.UtilTime;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CountdownBarTask implements Runnable {

    private long startTime;
    private int duration;

    private int taskId;

    public CountdownBarTask(long startTime, int duration) {
        this.startTime = startTime;
        this.duration = duration;
    }

    @Override
    public void run() {
        if (startTime < System.currentTimeMillis()) {
            for(Player p : UHC.arena.players()){
                BosssBarHandler.removeBar(p);
            }
            cancel();
            return;
        }
        for(Player p : UHC.arena.players()){
            double msLeft = startTime - System.currentTimeMillis();
            double percent = (msLeft / duration);
            BosssBarHandler.setBossBarText(p, ChatColor.WHITE+""+ ChatColor.BOLD+"Starting in "+ChatColor.GREEN+ChatColor.BOLD+ UtilTime.format(1, startTime - System.currentTimeMillis(), UtilTime.TimeUnit.FIT));
            BosssBarHandler.setBossBarProgress(p, percent);
        }
    }

    public void setTaskId(int id){
        this.taskId = id;
    }

    public void cancel(){
        Bukkit.getServer().getScheduler().cancelTask(this.taskId);
        UHC.arena.countdownTask = null;
    }
}
