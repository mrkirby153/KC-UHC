package me.mrkirby153.kcuhc.scoreboard;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.scoreboard.KirbyScoreboard;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.HashSet;
import java.util.Set;

public class UHCScoreboard extends KirbyScoreboard {

    private Objective tablistHealth;
    private Objective belowNameHealth;

    public UHCScoreboard() {
        super(ChatColor.GOLD + "" + ChatColor.BOLD + "KC UHC");
        tablistHealth = addObjective("TablistHealth", DisplaySlot.PLAYER_LIST, "health");
        char heart = '\u2764'; // heart
        tablistHealth = addObjective(ChatColor.RED + Character.toString(heart), DisplaySlot.BELOW_NAME, "health");
    }

    @Override
    public Set<ScoreboardTeam> getTeams() {
        return new HashSet<>(UHC.plugin.teamHandler.teams());
    }

    public Objective getTablistHealth() {
        return tablistHealth;
    }

    public Objective getBelowNameHealth() {
        return belowNameHealth;
    }
}
