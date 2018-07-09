package com.mrkirby153.kcuhc.game.team;

import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.Player;


public class UHCTeam extends ScoreboardTeam {

    private UHCGame game;

    public UHCTeam(String name, ChatColor color, UHCGame game) {
        super(name, color);
        this.game = game;
        setFriendlyFire(false);
        setSeeInvisible(true);
    }

    @Override
    public void removePlayer(Player player) {
        super.removePlayer(player);
        // TODO 7/8/2018: Fix switching teams not working while the game is running
        if (game.getCurrentState() == GameState.ALIVE) {
            ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(p -> {
                p.getTeam(this).ifPresent(t -> t.leaveTeam(player));
            });
        }
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(player);
        if (game.getCurrentState() == GameState.ALIVE) {
            ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(p -> {
                p.getTeam(this).ifPresent(t -> t.joinTeam(player));
            });
        }
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
