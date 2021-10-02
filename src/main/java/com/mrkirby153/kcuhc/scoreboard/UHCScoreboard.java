package com.mrkirby153.kcuhc.scoreboard;

import com.mrkirby153.kcuhc.Strings;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.msc.prestige.PrestigeModule;
import me.mrkirby153.kcutils.scoreboard.KirbyScoreboard;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class UHCScoreboard extends KirbyScoreboard {

    private UHC plugin;
    private UHCGame game;
    private Objective tablistHealth;
    private Objective belowNameHealth;

    public UHCScoreboard(UHC plugin) {
        super(ChatColor.GOLD + "" + ChatColor.BOLD + Strings.SHORT_NAME);
        this.plugin = plugin;
        this.game = plugin.getGame();

        tablistHealth = addObjective("TablistHealth", DisplaySlot.PLAYER_LIST, "health");
        belowNameHealth = addObjective(ChatColor.RED + Character.toString('\u2764'),
            DisplaySlot.BELOW_NAME, "dummy");
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
        if (game.getCurrentState() != GameState.ALIVE) {
            Optional<PrestigeModule> mod = ModuleRegistry.INSTANCE
                .getLoadedModule(PrestigeModule.class);
            mod.ifPresent(prestigeModule -> teams.addAll(prestigeModule.getTeams().values()));
            if (!mod.isPresent()) {
                teams.addAll(game.getTeams().values());
                teams.add(game.getSpectators());
            }
        } else {
            teams.addAll(game.getTeams().values());
            teams.add(game.getSpectators());
        }
        return teams;
    }

    /**
     * Adds a spacer
     */
    public void addSpacer() {
        this.add(" ");
    }
}
