package com.mrkirby153.kcuhc.game;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public class CountdownTimer implements Runnable {

    private int taskId;

    private int timer;

    private Consumer<Integer> function;

    private JavaPlugin plugin;

    public CountdownTimer(JavaPlugin plugin, int time, int interval, Consumer<Integer> function) {
        this.timer = time;
        this.plugin = plugin;
        taskId = plugin.getServer().getScheduler()
            .scheduleSyncRepeatingTask(plugin, this, 0L, interval);
        this.function = function;
    }

    @Override
    public void run() {
        function.accept(timer--);
        if (timer < 0) {
            plugin.getServer().getScheduler().cancelTask(this.taskId);
        }
    }
}
