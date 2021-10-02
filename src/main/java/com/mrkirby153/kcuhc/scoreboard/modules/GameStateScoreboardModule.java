package com.mrkirby153.kcuhc.scoreboard.modules;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardModule;
import com.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Module displaying the current game state
 */
public class GameStateScoreboardModule implements ScoreboardModule {

    private final UHCGame game;

    @Inject
    public GameStateScoreboardModule(UHCGame game) {
        this.game = game;
    }

    @Override
    public void drawScoreboard(UHCScoreboard scoreboard, Player player) {
        switch (game.getCurrentState()) {
            case COUNTDOWN:
            case WAITING:
                if (game.getCurrentState() == GameState.COUNTDOWN) {
                    scoreboard.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Starting...");
                } else {
                    scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "Waiting for start...");
                }
                scoreboard.addSpacer();
                scoreboard.add(
                    "Players Online: " + ChatColor.GOLD + Bukkit.getOnlinePlayers().size());
                break;
            case ALIVE:
                long aliveTeamCount = this.game.getTeams().values().stream()
                    .filter(t -> t.getPlayers().size() > 0).count();
                long totalTeams = this.game.getTeams().size();
                long alivePlayers = this.game.getTeams().values().stream()
                    .mapToLong(t -> t.getPlayers().size()).sum();
                scoreboard.add(ChatColor.GREEN + "Players Alive: " + ChatColor.RED + alivePlayers
                    + ChatColor.GRAY + "/" + ChatColor.WHITE + this.game.getInitialPlayers());
                scoreboard.add(ChatColor.GREEN + "Teams Alive: " + ChatColor.WHITE + aliveTeamCount
                    + ChatColor.GRAY + "/" + ChatColor.WHITE + totalTeams);
                scoreboard.addSpacer();
                scoreboard.add(
                    ChatColor.GRAY + "Kills: " + ChatColor.WHITE + this.game.getKills(player));
                break;
        }
    }

    @Override
    public boolean shouldDisplay() {
        return game.getCurrentState() == GameState.COUNTDOWN
            || game.getCurrentState() == GameState.WAITING
            || game.getCurrentState() == GameState.ALIVE;
    }
}
