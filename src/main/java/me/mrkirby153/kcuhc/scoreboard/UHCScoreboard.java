package me.mrkirby153.kcuhc.scoreboard;

import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class UHCScoreboard {

    private Scoreboard scoreboard;
    private Objective sideObjective;
    private Objective tablistHealth;
    private Objective belowNameHealth;

    private ArrayList<String> scoreboardElements = new ArrayList<>();
    private String[] current = new String[15];


    public UHCScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Random r = new Random();
        sideObjective = scoreboard.registerNewObjective("Obj-" + r.nextInt(99999999), "dummy");
        sideObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        sideObjective.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "KC UHC");
        tablistHealth = scoreboard.registerNewObjective("Obj-" + r.nextInt(99999999), "health");
        tablistHealth.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        belowNameHealth = scoreboard.registerNewObjective("Obj-" + r.nextInt(99999999), "health");
        belowNameHealth.setDisplaySlot(DisplaySlot.BELOW_NAME);
        char heart = '\u2764';
        belowNameHealth.setDisplayName(org.bukkit.ChatColor.RED + Character.toString(heart));
    }

    public void createTeams() {
        System.out.println("Creating scoreboard teams...");
        for (UHCTeam t : TeamHandler.teams()) {
            System.out.println("Creating team " + t.getName());
            if (scoreboard.getTeam(parseTeam(t.getName())) != null) {
                scoreboard.getTeam(parseTeam(t.getName())).unregister();
            }
            Team sbTeam = scoreboard.registerNewTeam(parseTeam(t.getName()));
            sbTeam.setPrefix(t.getColor() + "");
            sbTeam.setSuffix(ChatColor.RESET + "");
            sbTeam.setCanSeeFriendlyInvisibles(true);
            sbTeam.setAllowFriendlyFire(false);
        }
    }

    public void setPlayerTeam(Player player, String teamName) {
        System.out.println("Assigning player to team " + teamName);
        for (Team team : scoreboard.getTeams()) {
            team.removeEntry(player.getName());
        }
        String team = parseTeam(teamName);
        try {
            Team boardTeam = scoreboard.getTeam(team);
            if (boardTeam == null) {
                createTeams();
                try {
                    setPlayerTeam(player, teamName);
                } catch (StackOverflowError e) {
                    System.err.println("Team " + teamName + " does not exist!");
                }
            } else
                boardTeam.addEntry(player.getName());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not add player to team " + teamName);
        }
    }

    public void leaveTeam(Player player, String name) {
        name = parseTeam(name);
        for (Team t : scoreboard.getTeams()) {
            if (t.getName().equalsIgnoreCase(name)) {
                t.removeEntry(player.getName());
            }
        }
    }

    public void reset() {
        scoreboardElements.clear();

    }

    public void add(String text) {
        if(text.length() < 16){
            for(int i = 0; i < 15 - text.length(); i++){
                text += " ";
            }
        }
        this.scoreboardElements.add(text.substring(0, Math.min(text.length(), 40)));
    }

    public void draw() {
        ArrayList<String> newLines = new ArrayList<>();
        for (String line : scoreboardElements) {
            while (true) {
                boolean matched = false;
                for (String otherLines : newLines) {
                    if (line.equals(otherLines)) {
                        line += ChatColor.RESET;
                        matched = true;
                    }
                }
                if (!matched)
                    break;
            }
            newLines.add(line);
        }
        HashSet<Integer> toAdd = new HashSet<>();
        HashSet<Integer> toRemove = new HashSet<>();
        for (int i = 0; i < 15; i++) {
            if (i >= newLines.size()) {
                if (current[i] != null) {
                    toRemove.add(i);
                }
                continue;
            }

            if (current[i] == null || !current[i].equals(newLines.get(i))) {
                toRemove.add(i);
                toAdd.add(i);
            }
        }
        for (int i : toRemove) {
            if (current[i] != null) {
                resetScore(current[i]);
                current[i] = null;
            }
        }
        for (int i : toAdd) {
            String newLine = newLines.get(i);
            sideObjective.getScore(newLine).setScore(15 - i);
            current[i] = newLine;
        }
    }

    private void resetScore(String line) {
        scoreboard.resetScores(line);
    }

    private String parseTeam(String name) {
        return name.substring(0, Math.min(name.length(), 16));
    }

    public Scoreboard getBoard() {
        return scoreboard;
    }
}
