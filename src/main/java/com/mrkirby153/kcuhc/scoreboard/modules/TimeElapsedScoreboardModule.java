package com.mrkirby153.kcuhc.scoreboard.modules;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardModule;
import com.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TimeElapsedScoreboardModule implements ScoreboardModule {

    private final UHCGame game;

    @Inject
    public TimeElapsedScoreboardModule(UHCGame game) {
        this.game = game;
    }

    @Override
    public void drawScoreboard(UHCScoreboard scoreboard, Player player) {
        scoreboard.add(new ElementHeadedText(
            ChatColor.GREEN + "" + ChatColor.BOLD + "Time Elapsed", Time.format(1,
            System.currentTimeMillis() - game.getStartTime())));
    }

    @Override
    public boolean shouldDisplay() {
        return game.getCurrentState() == GameState.ALIVE;
    }
}
