package me.mrkirby153.kcuhc.team;

import com.google.common.base.Throwables;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.Module;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TeamHandler extends Module<UHC> {

    public static final String SPECTATOR_TEAM_NAME = "spectators";

    private HashMap<UUID, UHCTeam> playerToTeamMap = new HashMap<>();
    private Set<UHCTeam> teams = new HashSet<>();
    private TeamSpectator spectatorTeam;

    public TeamHandler(UHC plugin) {
        super("Team Handler", "1.0", plugin);
    }

    public UHCTeam getTeamByName(String name) {
        List<UHCTeam> array = teams.stream().filter(team -> team.getTeamName().equalsIgnoreCase(name)).collect(Collectors.toList());
        if(array.size() > 0)
            return array.get(0);
        else
            return null;
    }

    public UHCTeam getTeamForPlayer(Player player) {
        return playerToTeamMap.get(player.getUniqueId());
    }

    public boolean isSpectator(Player player) {
        return getTeamForPlayer(player) == spectatorTeam;
    }

    public void joinTeam(UHCTeam team, Player player) {
        if (getTeamForPlayer(player) != null) {
            teams.stream().filter(t -> t.getPlayers().contains(player.getUniqueId())).forEach(t -> {
                t.removePlayer(player);
            });
        }
        team.addPlayer(player);
        playerToTeamMap.put(player.getUniqueId(), team);
        UHC.arena.addPlayer(player);
    }

    public void leaveTeam(Player player) {
        UHCTeam team = playerToTeamMap.remove(player.getUniqueId());
        if (team != null) {
            team.removePlayer(player);
            team.onLeave(player);
        }
    }

    public void registerTeam(UHCTeam team) {
        teams.add(team);
    }

    public synchronized void saveTeam(UHCTeam team) {
        File file = new File(UHC.plugin.getDataFolder(), "teams.yml");
        YamlConfiguration config;
        if (!file.exists())
            config = new YamlConfiguration();
        else
            config = YamlConfiguration.loadConfiguration(file);
        config.set(team.getTeamName(), team);
        try {
            config.save(file);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public synchronized void saveToFile() {
        for (UHCTeam t : teams()) {
            if (t == spectatorsTeam()) {
                continue;
            }
            saveTeam(t);
        }
    }

    public Collection<UHCTeam> teams(boolean spectator) {
        return teams.stream().filter(t -> spectator || t instanceof UHCPlayerTeam).collect(Collectors.toList());
    }

    public Collection<UHCTeam> teams() {
        return teams(true);
    }

    public void unregisterAll() {
        Set<UHCTeam> teams = new HashSet<>(this.teams);
        teams.forEach(this::unregisterTeam);
    }

    public void unregisterTeam(String name) {
        teams.removeIf(uhcTeam -> uhcTeam.getTeamName().equals(name));
    }

    public void unregisterTeam(UHCTeam team) {
        teams.removeIf(uhcTeam -> uhcTeam == team);
    }

    public TeamSpectator spectatorsTeam() {
        return spectatorTeam;
    }

    @Override
    protected void init() {
        // Register the spectators team
        loadFromFile();
        registerTeam(spectatorTeam = new TeamSpectator(this));
    }
    public void loadFromFile() {
        unregisterAll();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(UHC.plugin.getDataFolder(), "teams.yml"));
        for (String s : config.getKeys(false)) {
            registerTeam((UHCPlayerTeam) config.get(s));
        }
    }
    /*public static final String SPECTATORS_TEAM = "spectators";

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
    }*/
}
