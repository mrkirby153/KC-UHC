package me.mrkirby153.kcuhc.team;

import com.google.common.base.Throwables;
import me.mrkirby153.kcuhc.UHC;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static org.spigotmc.SpigotConfig.config;

public class TeamHandler {

    public static final String SPECTATORS_TEAM = "spectators";

    private static HashMap<UUID, UHCTeam> playerToTeamMap = new HashMap<>();
    private static HashMap<String, UHCTeam> teamNameToTeamMap = new HashMap<>();

    public static void joinTeam(UHCTeam team, Player player) {
        if (getTeamForPlayer(player) != null) {
            for (UHCTeam t : teams()) {
                if (t.getPlayers().contains(player.getUniqueId())) {
                    System.out.println("Removing " + player.getName() + " from " + team.getName());
                    t.removePlayer(player);
                }
            }
            leaveTeam(player);
        }
        team.addPlayer(player);
        UHC.arena.scoreboard.setPlayerTeam(player, team.getName());
        playerToTeamMap.put(player.getUniqueId(), team);
        UHC.arena.addPlayer(player);
    }

    public static void leaveTeam(Player player) {
        UHCTeam team = playerToTeamMap.remove(player.getUniqueId());
        if (team != null) {
/*            Team t = board.getTeam(team.getScoreboardName());
            if (t != null) {
                t.removeEntry(player.getName());
            }*/
            UHC.arena.scoreboard.leaveTeam(player, team.getName());
            team.removePlayer(player);
            team.onLeave(player);
            System.out.println("Removing " + player.getName() + " from team " + team.getName());
        }
    }

    public static void registerTeam(String name, UHCTeam team) {
        teamNameToTeamMap.put(name, team);
/*        Team boardTeam = board.getTeam(team.getScoreboardName());
        if (boardTeam != null)
            boardTeam.unregister();
        Team newTeam = board.registerNewTeam(team.getScoreboardName());
        newTeam.setPrefix(ChatColor.COLOR_CHAR + "" + getCode(team.getColor()));
        newTeam.setSuffix(ChatColor.RESET + "");
        newTeam.setCanSeeFriendlyInvisibles(true);
        newTeam.setAllowFriendlyFire(false);*/
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

    public static void unregisterTeam(String name) {
////        Team t = board.getTeam(name);
//        if (t != null) {
//            System.out.println("Unregsitering team " + name);
//            t.unregister();
//        }
        Iterator<Map.Entry<String, UHCTeam>> i = teamNameToTeamMap.entrySet().iterator();
        while (i.hasNext()) {
            if (i.next().getKey().equals(name))
                i.remove();
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

    public static void loadFromFile() {
        unregisterAll();
        TeamHandler.registerTeam(TeamHandler.SPECTATORS_TEAM, new TeamSpectator());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(UHC.plugin.getDataFolder(), "teams.yml"));
        for (String s : config.getKeys(false)) {
            registerTeam(s, (UHCPlayerTeam) config.get(s));
        }
    }

    public static void unregisterTeam(UHCTeam teamByName) {
        unregisterTeam(teamByName.getName());
    }

    public static void unregisterAll() {
        Object clone = teamNameToTeamMap.clone();
        if (!(clone instanceof Map))
            return;
        @SuppressWarnings("unchecked")
        Map<String, UHCTeam> teams = (Map<String, UHCTeam>) clone;
        teams.values().forEach(TeamHandler::unregisterTeam);
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
