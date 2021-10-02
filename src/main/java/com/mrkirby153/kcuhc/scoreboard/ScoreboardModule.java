package com.mrkirby153.kcuhc.scoreboard;

import org.bukkit.entity.Player;

/**
 * A pluggable module for the UHC scoreboard
 */
public interface ScoreboardModule {

    /**
     * Draw the scoreboard for this module
     * @param scoreboard The scoreboard to draw
     * @param player The player to draw the scoreboard for
     */
    void drawScoreboard(UHCScoreboard scoreboard, Player player);

    /**
     * If this module should be rendered
     * @return True if the module should be rendered
     */
    boolean shouldDisplay();
}
