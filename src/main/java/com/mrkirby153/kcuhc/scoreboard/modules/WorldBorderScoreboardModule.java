package com.mrkirby153.kcuhc.scoreboard.modules;

import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardModule;
import com.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class WorldBorderScoreboardModule implements ScoreboardModule {

    private final UHCGame game;
    private final WorldBorderModule module;

    public WorldBorderScoreboardModule(UHCGame game, WorldBorderModule module) {
        this.game = game;
        this.module = module;
    }

    @Override
    public void drawScoreboard(UHCScoreboard scoreboard, Player player) {
        double worldBorderLocation = module.worldborderLoc()[0];
        scoreboard.add(
            new ElementHeadedText(ChatColor.YELLOW + "" + ChatColor.BOLD + "World Border",
                String.format("from -%.1f to +%.1f", worldBorderLocation, worldBorderLocation)));
    }

    @Override
    public boolean shouldDisplay() {
        return game.getCurrentState() == GameState.ALIVE;
    }
}
