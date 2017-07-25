package com.mrkirby153.kcuhc.game.team;

import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;

public class UHCTeam extends ScoreboardTeam {

    public UHCTeam(String name, ChatColor color) {
        super(name, color);
        friendlyFire = false;
        seeInvisible = true;
    }
}
