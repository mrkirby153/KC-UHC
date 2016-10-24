package me.mrkirby153.kcuhc.team;

import com.google.common.base.Throwables;
import me.mrkirby153.kcuhc.UHC;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.spigotmc.SpigotConfig.config;

public class TeamHandler {

    public static final String SPECTATORS_TEAM = "spectators";

    private static HashMap<UUID, UHCTeam> playerToTeamMap = new HashMap<>();
    private static HashMap<String, UHCTeam> teamNameToTeamMap = new HashMap<>();

    public static UHCTeam getTeamByName(String name) {
        return teamNameToTeamMap.get(name);
    }

    public static UHCTeam getTeamForPlayer(Player player) {
        return playerToTeamMap.get(player.getUniqueId());
    }

    public static boolean isSpectator(Player player) {
        return getTeamForPlayer(player) == spectatorsTeam();
    }

    public static void joinTeam(UHCTeam team, Player player) {
        if (getTeamForPlayer(player) != null) {
            teams().stream().filter(t -> t.getPlayers().contains(player.getUniqueId())).forEach(t -> {
                System.out.println("Removing " + player.getName() + " from " + team.getName());
                t.removePlayer(player);
            });
            leaveTeam(player);
        }
        team.addPlayer(player);
        UHC.arena.scoreboardUpdater.getScoreboard().setPlayerTeam(player, team.getName());
        playerToTeamMap.put(player.getUniqueId(), team);
        UHC.arena.addPlayer(player);
    }

    public static void leaveTeam(Player player) {
        UHCTeam team = playerToTeamMap.remove(player.getUniqueId());
        if (team != null) {
            UHC.arena.scoreboardUpdater.getScoreboard().leaveTeam(player, team.getName());
            team.removePlayer(player);
            team.onLeave(player);
            System.out.println("Removing " + player.getName() + " from team " + team.getName());
        }
    }

    public static void loadFromFile() {
        unregisterAll();
        TeamHandler.registerTeam(TeamHandler.SPECTATORS_TEAM, new TeamSpectator());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(UHC.plugin.getDataFolder(), "teams.yml"));
        for (String s : config.getKeys(false)) {
            registerTeam(s, (UHCPlayerTeam) config.get(s));
        }
    }

    public static void registerTeam(String name, UHCTeam team) {
        if (teamNameToTeamMap.containsKey(name)) {
            unregisterTeam(name);
        }
        teamNameToTeamMap.put(name, team);
    }

    public static void registerTeam(String name, ChatColor color) {
        registerTeam(name, new UHCPlayerTeam(name, color));
    }

    public synchronized static void saveTeam(UHCTeam team) {
        File file = new File(UHC.plugin.getDataFolder(), "teams.yml");
        YamlConfiguration config;
        if (!file.exists())
            config = new YamlConfiguration();
        else
            config = YamlConfiguration.loadConfiguration(file);
        config.set(team.getName(), team);
        try {
            config.save(file);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public synchronized static void saveToFile() {
        for (UHCTeam t : teams()) {
            if (t == TeamHandler.spectatorsTeam()) {
                continue;
            }
            saveTeam(t);
            config.set(t.getName(), t);
        }
    }

    public static UHCTeam spectatorsTeam() {
        return getTeamByName(SPECTATORS_TEAM);
    }

    public static Collection<UHCTeam> teams(boolean spectator) {
        return teamNameToTeamMap.values().stream().filter(t -> spectator || t instanceof UHCPlayerTeam).collect(Collectors.toList());
    }

    public static Collection<UHCTeam> teams(){
        return teams(false);
    }

    public static void unregisterAll() {
        @SuppressWarnings("unchecked")
        Map<String, UHCTeam> teams = new HashMap<>(teamNameToTeamMap);
        teams.values().forEach(TeamHandler::unregisterTeam);
    }

    public static void unregisterTeam(String name) {
        Iterator<Map.Entry<String, UHCTeam>> i = teamNameToTeamMap.entrySet().iterator();
        while (i.hasNext()) {
            if (i.next().getKey().equals(name))
                i.remove();
        }
    }

    public static void unregisterTeam(UHCTeam teamByName) {
        unregisterTeam(teamByName.getName());
    }
}
