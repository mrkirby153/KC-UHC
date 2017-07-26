package com.mrkirby153.kcuhc.scoreboard;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.scoreboard.KirbyScoreboard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * The scoreboard updater
 */
public class ScoreboardUpdater implements Listener {

    private final KirbyScoreboard scoreboard;
    private final UHC plugin;
    private final UHCGame game;

    public ScoreboardUpdater(UHC plugin) {
        this.plugin = plugin;
        this.scoreboard = new UHCScoreboard(plugin);
        this.game = plugin.getGame();
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.FAST)
            return;
        // Update scoreboard
        Bukkit.getOnlinePlayers().stream().filter(p -> p.getScoreboard() != scoreboard.getBoard())
                .forEach(p -> p.setScoreboard(scoreboard.getBoard()));
        scoreboard.reset();
        switch (this.game.getCurrentState()) {
            case COUNTDOWN:
            case WAITING:
                scoreboard.add(" ");
                if (game.getCurrentState() == GameState.COUNTDOWN)
                    scoreboard.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Starting...");
                else
                    scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "Waiting for start...");
                scoreboard.add(" ");
                scoreboard.add("Players Online: " + ChatColor.GOLD + Bukkit.getOnlinePlayers().size());
                scoreboard.add(" ");
                scoreboard.add(" ");
                break;
        }
        scoreboard.draw();
    }
}
