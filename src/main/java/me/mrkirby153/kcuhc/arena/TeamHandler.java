package me.mrkirby153.kcuhc.arena;

import com.google.common.base.Throwables;
import me.mrkirby153.kcuhc.UHC;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

public class TeamHandler {

    public static final String SPECTATORS_TEAM = "spectators";

    protected static Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
    private static HashMap<UUID, UHCTeam> playerToTeamMap = new HashMap<>();
    private static HashMap<String, UHCTeam> teamNameToTeamMap = new HashMap<>();

    public static void joinTeam(UHCTeam team, Player player) {
        if (getTeamForPlayer(player) != null)
            leaveTeam(player);
        team.addPlayer(player);
        Team t = board.getTeam(team.getScoreboardName());
        if (t != null)
            t.addEntry(player.getName());
        playerToTeamMap.put(player.getUniqueId(), team);
        UHC.arena.addPlayer(player);
    }

    public static void leaveTeam(Player player) {
        UHCTeam team = playerToTeamMap.remove(player.getUniqueId());
        if (team != null) {
            Team t = board.getTeam(team.getScoreboardName());
            if (t != null) {
                t.removeEntry(player.getName());
            }
            team.removePlayer(player);
            team.onLeave(player);
            System.out.println("Removing " + player.getName() + " from team " + team.getName());
        }
    }

    public static void registerTeam(String name, UHCTeam team) {
        teamNameToTeamMap.put(name, team);
        Team boardTeam = board.getTeam(team.getScoreboardName());
        if (boardTeam != null)
            boardTeam.unregister();
        Team newTeam = board.registerNewTeam(team.getScoreboardName());
        newTeam.setPrefix(ChatColor.COLOR_CHAR + "" + getCode(team.getColor()));
        newTeam.setSuffix(ChatColor.RESET + "");
        newTeam.setCanSeeFriendlyInvisibles(true);
        newTeam.setAllowFriendlyFire(false);
    }

    public static UHCTeam getTeamByName(String name) {
        return teamNameToTeamMap.get(name);
    }

    public static UHCTeam getTeamForPlayer(Player player) {
        return playerToTeamMap.get(player.getUniqueId());
    }

    public static Collection<UHCTeam> teams() {
        return teamNameToTeamMap.values();
    }

    public static void unregisterTeam(String name, boolean remove) {
        Team t = board.getTeam(name);
        if (t != null) {
            System.out.println("Unregsitering team " + name);
            t.unregister();
        }
        if (remove)
            teamNameToTeamMap.remove(name);
    }

    public static void unregisterTeam(String name) {
        unregisterTeam(name, true);
    }

    public static void unregisterTeam(UHCTeam teamByName, boolean remove) {
        unregisterTeam(teamByName.getScoreboardName(), remove);
    }

    public static void saveToFile() {
        YamlConfiguration config = new YamlConfiguration();
        for (UHCTeam t : teams()) {
            if (t == TeamHandler.spectatorsTeam())
                continue;
            config.set(t.getName(), t);
        }
        try {
            config.save(new File(UHC.plugin.getDataFolder(), "teams.yml"));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public static void loadFromFile() {
        unregisterAll();
        TeamHandler.registerTeam(TeamHandler.SPECTATORS_TEAM, new TeamSpectator());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(UHC.plugin.getDataFolder(), "teams.yml"));
        for (String s : config.getKeys(false)) {
            registerTeam(s, (UHCPlayerTeam) config.get(s));
        }
        // Save teams to convert names to UUIDs
        saveToFile();
    }

    public static void unregisterTeam(UHCTeam teamByName) {
        unregisterTeam(teamByName.getName());
    }

    public static void unregisterAll() {
        Iterator<UHCTeam> team = teamNameToTeamMap.values().iterator();
        while (team.hasNext()) {
            unregisterTeam(team.next(), false);
            team.remove();
        }
    }

    private static char getCode(net.md_5.bungee.api.ChatColor color) {
        try {
            Field f = color.getClass().getDeclaredField("code");
            f.setAccessible(true);
            return (char) f.get(color);
        } catch (Exception e) {
            return 'f';
        }
    }

    public static UHCTeam spectatorsTeam() {
        return getTeamByName(SPECTATORS_TEAM);
    }

    public static boolean isSpectator(Player player) {
        return getTeamForPlayer(player) == spectatorsTeam();
    }
}
