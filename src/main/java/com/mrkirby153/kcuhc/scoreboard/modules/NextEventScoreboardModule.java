package com.mrkirby153.kcuhc.scoreboard.modules;

import com.mrkirby153.kcuhc.game.EventTracker;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardModule;
import com.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.Time.TimeUnit;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class NextEventScoreboardModule implements ScoreboardModule {

    private final EventTracker eventTracker;

    public NextEventScoreboardModule(EventTracker eventTracker) {
        this.eventTracker = eventTracker;
    }

    @Override
    public void drawScoreboard(UHCScoreboard scoreboard, Player player) {
        eventTracker.getNextEvent().ifPresent(e -> {
            scoreboard.add(new ElementHeadedText(
                ChatColor.AQUA + "" + ChatColor.BOLD + e.getEvent().getName() + " in",
                Time.format(1, e.getMsLeft(), TimeUnit.FIT, TimeUnit.SECONDS)));
        });
    }

    @Override
    public boolean shouldDisplay() {
        return eventTracker.getNextEvent().isPresent();
    }
}
