package me.mrkirby153.kcuhc.team;

import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Color;
import org.bukkit.entity.Player;

public abstract class UHCTeam extends ScoreboardTeam {

    private ChatColor color;

    private String friendlyName;

    public UHCTeam(String name, ChatColor color) {
        super(name, color);
        this.friendlyName = WordUtils.capitalize(name.replace("_", " "));
        this.color = color;
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(player);
        player.sendMessage(UtilChat.message("You are now on team " + color + friendlyName));
        onJoin(player);
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String name) {
        this.friendlyName = name;
    }

    public abstract void onJoin(Player player);

    public abstract void onLeave(Player player);

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
                return Color.YELLOW;
        }
    }
}
