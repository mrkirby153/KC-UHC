package com.mrkirby153.kcuhc.scoreboard.modules;

import com.mrkirby153.kcuhc.module.player.PvPGraceModule;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardModule;
import com.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.Time.TimeUnit;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PvpGraceScoreboardModule implements ScoreboardModule {

    private final PvPGraceModule module;

    public PvpGraceScoreboardModule(PvPGraceModule module) {
        this.module = module;
    }

    @Override
    public void drawScoreboard(UHCScoreboard scoreboard, Player player) {
        scoreboard.add(
            new ElementHeadedText(ChatColor.AQUA + "" + ChatColor.BOLD + "PvP Enabled in",
                Time.formatLong(module.getGraceTimeRemaining(), TimeUnit.SECONDS)));
    }

    @Override
    public boolean shouldDisplay() {
        return module.getGraceTimeRemaining() > 0;
    }
}
