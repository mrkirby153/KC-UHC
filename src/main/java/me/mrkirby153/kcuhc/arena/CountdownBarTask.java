package me.mrkirby153.kcuhc.arena;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilTime;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class CountdownBarTask implements Runnable {

    private long startTime;
    private int duration;

    private BossBar bossBar;

    private int taskId;

    public CountdownBarTask(long startTime, int duration) {
        this.startTime = startTime;
        this.duration = duration;
        bossBar = Bukkit.createBossBar(ChatColor.WHITE + "" + ChatColor.BOLD + "Starting in ", BarColor.PINK, BarStyle.SOLID);
        bossBar.setProgress(1);
    }

    @Override
    public void run() {
        if (startTime < System.currentTimeMillis()) {
            bossBar.removeAll();
            cancel();
            return;
        }
        for (Player p : UHC.arena.players()) {
            if (!bossBar.getPlayers().contains(p))
                bossBar.addPlayer(p);
        }
        bossBar.setTitle(ChatColor.WHITE + "" + ChatColor.BOLD + "Starting in " + ChatColor.GREEN + ChatColor.BOLD + UtilTime.format(1, startTime - System.currentTimeMillis(), UtilTime.TimeUnit.FIT));
        double msLeft = startTime - System.currentTimeMillis();
        double percent = (msLeft / duration);
        bossBar.setProgress(percent);
    }

    public void setTaskId(int id){
        this.taskId = id;
    }

    public void cancel(){
        Bukkit.getServer().getScheduler().cancelTask(this.taskId);
    }
}
