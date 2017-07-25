package com.mrkirby153.kcuhc.game;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;

/**
 * The main game class
 */
public class UHCGame {
    /**
     * The current state of the game
     */
    private GameState currentState = GameState.WAITING;

    /**
     * The main plugin instance
     */
    private UHC plugin;

    /**
     * A map of all the teams currently in the game
     */
    private HashMap<String, UHCTeam> teams = new HashMap<>();

    /**
     * The spectator team
     */
    private SpectatorTeam spectators = new SpectatorTeam();

    public UHCGame(UHC plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a team
     *
     * @param name  The name of the team
     * @param color The color of the team
     * @return The created team
     */
    public UHCTeam createTeam(String name, ChatColor color) {
        UHCTeam team = new UHCTeam(name, color);
        this.teams.put(name.toLowerCase(), team);
        return team;
    }

    public void deleteTeam(UHCTeam team) {
        this.teams.entrySet().removeIf(teamEntry -> teamEntry.getValue().equals(team));
    }

    /**
     * Gets the current state of the game
     *
     * @return The current state of the game
     */
    public GameState getCurrentState() {
        return currentState;
    }

    /**
     * Sets the current state of the game, firing {@link com.mrkirby153.kcuhc.game.event.GameStateChangeEvent}
     *
     * @param newState The new state of the game
     */
    public void setCurrentState(GameState newState) {
        plugin.getLogger().info(String.format("[GAME STATE] Changing from %s to %s", this.currentState, newState));
        this.currentState = newState;
        Bukkit.getServer().getPluginManager().callEvent(new GameStateChangeEvent(newState));
    }

    /**
     * Gets the spectator team
     *
     * @return The spectator team
     */
    public SpectatorTeam getSpectators() {
        return spectators;
    }

    /**
     * Gets a team by its name
     *
     * @param name The name of the team
     * @return The team, or null if it doesn't exist
     */
    public UHCTeam getTeam(String name) {
        return teams.get(name.toLowerCase());
    }

    /**
     * Gets the team the player is on
     *
     * @param player The Player to get the team for
     * @return The team, or null if the player isn't on a team
     */
    public ScoreboardTeam getTeam(Player player) {
        for (UHCTeam team : this.teams.values()) {
            if (team.getPlayers().contains(player.getUniqueId()))
                return team;
        }
        if (this.spectators.getPlayers().contains(player.getUniqueId()))
            return this.spectators;
        return null;
    }

    /**
     * Gets all the teams currently registered
     *
     * @return The team
     */
    public HashMap<String, UHCTeam> getTeams() {
        return teams;
    }
}
