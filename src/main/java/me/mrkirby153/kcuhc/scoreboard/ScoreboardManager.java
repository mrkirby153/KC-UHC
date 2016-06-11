package me.mrkirby153.kcuhc.scoreboard;

import me.mrkirby153.kcuhc.UHC;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class ScoreboardManager implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerLogin(PlayerLoginEvent event) {
        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED)
            Bukkit.getServer().getScheduler().runTaskLater(UHC.plugin, () -> event.getPlayer().setScoreboard(UHC.arena.scoreboardUpdater.getScoreboard().getBoard()), 10);
    }
}
