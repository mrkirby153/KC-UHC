package com.mrkirby153.kcuhc.game.team;

import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

public class SpectatorTeam extends ScoreboardTeam {

    public SpectatorTeam() {
        super("Spectators", ChatColor.GRAY);
        friendlyFire = false;
        seeInvisible = true;
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(player);
        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(player));
        players.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> {
            player.showPlayer(p);
            p.showPlayer(player);
        });
    }

    @Override
    public void removePlayer(Player player) {
        super.removePlayer(player);
        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(player));
        players.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(player::hidePlayer);
    }
}
