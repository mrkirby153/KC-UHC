package com.mrkirby153.kcuhc.game.team;

import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;


public class UHCTeam extends ScoreboardTeam {

    public UHCTeam(String name, ChatColor color) {
        super(name, color);
        friendlyFire = false;
        seeInvisible = true;
    }

    public Color toColor() {
        switch (getColor()) {
            case WHITE:
                return Color.WHITE;
            case GRAY:
                return Color.SILVER;
            case DARK_GRAY:
                return Color.GRAY;
            case BLACK:
                return Color.BLACK;
            case RED:
                return Color.RED;
            case YELLOW:
                return Color.YELLOW;
            case GREEN:
                return Color.LIME;
            case DARK_GREEN:
                return Color.GREEN;
            case AQUA:
                return Color.AQUA;
            case DARK_AQUA:
                return Color.TEAL;
            case DARK_BLUE:
                return Color.NAVY;
            case BLUE:
                return Color.BLUE;
            case DARK_PURPLE:
                return Color.MAROON;
            case DARK_RED:
                return Color.RED;
            case GOLD:
                return Color.ORANGE;
            case LIGHT_PURPLE:
                return Color.FUCHSIA;
            default:
                return Color.GREEN;
        }
    }
}
