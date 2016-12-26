package me.mrkirby153.kcuhc.arena;

import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.worldborder.EndgameModule;
import me.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilTime;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ScoreboardUpdater {

    private final UHCScoreboard scoreboard;
    private final UHCArena arena;
    private final TeamHandler teamHandler;

    public ScoreboardUpdater(UHCArena arena, TeamHandler teamHandler, UHCScoreboard scoreboard) {
        this.scoreboard = scoreboard;
        this.teamHandler = teamHandler;
        this.arena = arena;
    }

    public UHCScoreboard getScoreboard() {
        return scoreboard;
    }

    public void refresh() {
        Bukkit.getOnlinePlayers().stream().filter(p -> p.getScoreboard() != scoreboard.getBoard()).forEach(p -> p.setScoreboard(scoreboard.getBoard()));
        scoreboard.reset();

        switch (arena.currentState()) {
            case WAITING:
            case INITIALIZED:
            case COUNTDOWN:
                scoreboard.add(" ");
                if (arena.currentState() == UHCArena.State.WAITING || arena.currentState() == UHCArena.State.INITIALIZED)
                    scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "Waiting for start...");
                else
                    scoreboard.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Starting....");
                scoreboard.add(" ");
                scoreboard.add("Players: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
                scoreboard.add(" ");
                break;
            case FROZEN:
                scoreboard.add(" ");
                scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "The game is frozen");
                scoreboard.add(" ");
                break;
            case RUNNING:
                List<Player> players = arena.players(false);
                // Sort the player list by health
                players.sort(((o1, o2) -> (int) Math.floor(o2.getHealth() - o1.getHealth())));
                // Offline players
                List<OfflinePlayer> offlinePlayers = arena.getDisconnectedPlayers();
                int spacesNeeded = players.size() + offlinePlayers.size();
                if (spacesNeeded < 9) {
                    for (Player p : players) {
                        UHCTeam team = teamHandler.getTeamForPlayer(p);
                        scoreboard.add(ChatColor.RED + "" + (int) +p.getHealth() + " " + (team != null ? team.getColor() : ChatColor.WHITE) + p.getName());
                    }
                    for (OfflinePlayer p : offlinePlayers) {
                        scoreboard.add(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + p.getName()+ ChatColor.RESET);
                    }
                } else {
                    // Show the number of teams
                    List<UHCTeam> teamsIngame = teamHandler.teams().stream().filter(team -> team.getPlayers().stream()
                            .map(Bukkit::getPlayer).filter(Objects::nonNull).count() > 0).collect(Collectors.toList());
                    // Sort the teams by size
                    teamsIngame.sort(((o1, o2) -> o2.getPlayers().size() - o1.getPlayers().size()));
                    scoreboard.add(ChatColor.AQUA + "Teams: ");
                    if (teamsIngame.size() > 8) {
                        scoreboard.add(ChatColor.GREEN + "" + teamsIngame.size() + ChatColor.WHITE + " alive");
                        if (offlinePlayers.size() > 0)
                            scoreboard.add(offlinePlayers.size() + "" + ChatColor.GRAY + " players offline");
                    } else {
                        teamsIngame.forEach(t -> {
                            long online = t.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).count();
                            scoreboard.add(online + " " + t.getColor() + t.getFriendlyName());
                        });
                        if (offlinePlayers.size() > 0)
                            scoreboard.add(offlinePlayers.size() + "" + ChatColor.GRAY + " players offline");
                    }

                }
                scoreboard.add(" ");
                ModuleRegistry.getLoadedModule(WorldBorderModule.class).ifPresent(worldBorderModule -> {
                    double[] wbPos = worldBorderModule.worldborderLoc();
                    scoreboard.add(new ElementHeadedText(ChatColor.YELLOW + "" + ChatColor.BOLD + "World Border:",
                            "from -" + UtilTime.trim(1, wbPos[0]) + " to +" + UtilTime.trim(1, wbPos[0])));
                });

                // Display endgame on scoreboard
                ModuleRegistry.getLoadedModule(EndgameModule.class).ifPresent(endgameHandler -> {
                    if (endgameHandler.getNextEndgamePhaseOn() != -1) {
                        scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "" +
                                ((endgameHandler.getNextEndgamePhase() == EndgameModule.EndgamePhase.UNKNOWN) ? endgameHandler.getCurrentEndgamePhase().getName() :
                                        endgameHandler.getNextEndgamePhase().getName()));
                        if (endgameHandler.getNextEndgamePhase() == EndgameModule.EndgamePhase.UNKNOWN) {
                            scoreboard.add("   ACTIVE");
                        } else {
                            scoreboard.add("  in " + UtilTime.format(1, endgameHandler.getNextEndgamePhaseOn() - System.currentTimeMillis(), UtilTime.TimeUnit.FIT));
                        }
                        scoreboard.add(" ");
                    }
                });
                // End display endgame on scoreboard

                scoreboard.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Time Elapsed");
                scoreboard.add(" " + UtilTime.format(1, System.currentTimeMillis() - arena.startTime, UtilTime.TimeUnit.FIT));
                break;
            case ENDGAME:
                scoreboard.add(" ");
                scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "Ended");
                scoreboard.add(" ");
                scoreboard.add(ChatColor.GOLD + "Winner:");
                scoreboard.add("   " + arena.winner);
                scoreboard.add(" ");
        }
        scoreboard.draw();
    }
}
