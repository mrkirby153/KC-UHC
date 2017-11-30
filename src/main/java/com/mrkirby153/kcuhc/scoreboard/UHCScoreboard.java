package com.mrkirby153.kcuhc.scoreboard;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.UHCGame;
import me.mrkirby153.kcutils.scoreboard.KirbyScoreboard;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class UHCScoreboard extends KirbyScoreboard {

    private UHC plugin;
    private UHCGame game;
    private Objective tablistHealth;
    private Objective belowNameHealth;

    public UHCScoreboard(UHC plugin) {
        super(ChatColor.GOLD + "" + ChatColor.BOLD + "KC UHC");
        this.plugin = plugin;
        this.game = plugin.getGame();

        tablistHealth = addObjective("TablistHealth", DisplaySlot.PLAYER_LIST, "health");
        belowNameHealth = addObjective(ChatColor.RED + Character.toString('\u2764'),
            DisplaySlot.BELOW_NAME, "health");
    }

    public Objective getBelowNameHealth() {
        return belowNameHealth;
    }

    public Objective getTablistHealth() {
        return tablistHealth;
    }

    @Override
    @NotNull
    public Set<ScoreboardTeam> getTeams() {
        HashSet<ScoreboardTeam> teams = new HashSet<>();
        teams.addAll(game.getTeams().values());
        teams.add(game.getSpectators());
        return teams;
    }
}
