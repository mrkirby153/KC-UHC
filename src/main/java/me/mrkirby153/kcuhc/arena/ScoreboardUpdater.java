package me.mrkirby153.kcuhc.arena;

import me.mrkirby153.kcuhc.arena.handler.EndgameHandler;
import me.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilTime;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
                scoreboard.add(" ");
                scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "Waiting for start...");
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
                List<UUID> players = Arrays.stream(arena.players()).map(Entity::getUniqueId).filter(u -> !teamHandler.spectatorsTeam().getPlayers().contains(u)).collect(Collectors.toList());
                players.sort((o1, o2) -> {
                    Player p1 = Bukkit.getPlayer(o1);
                    Player p2 = Bukkit.getPlayer(o2);
                    if (p1 == null)
                        return 1;
                    if (p2 == null)
                        return -1;
                    return (int) Math.floor(p2.getHealth() - p1.getHealth());
                });
                int spacesNeeded = players.size();
                if (spacesNeeded < 9) {
                    for (UUID u : players) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(u);
                        Player onlinePlayer = null;
                        UHCTeam team;
                        if (op instanceof Player) {
                            team = teamHandler.getTeamForPlayer((Player) op);
                            onlinePlayer = (Player) op;
                        } else {
                            team = null;
                        }
                        if (team == null) {
                            scoreboard.add(ChatColor.GRAY + op.getName());
                        } else {
                            scoreboard.add(ChatColor.RED + "" + (int) +onlinePlayer.getHealth() + " " + team.getColor() + op.getName());
                        }
                    }
                } else {
                    List<UHCTeam> teamsIngame = teamHandler.teams().stream().filter(team -> team.getPlayers().stream().map(Bukkit::getPlayer).filter(p -> p != null).count() > 0).collect(Collectors.toList());
                    teamsIngame.sort((t1, t2) -> t1.getPlayers().size() - t2.getPlayers().size());
                    scoreboard.add(ChatColor.AQUA + "Teams: ");
                    if (teamsIngame.size() > 9) {
                        scoreboard.add(ChatColor.GREEN + "" + teamsIngame.size() + ChatColor.WHITE + " alive");
                    } else {
                        teamsIngame.forEach(t -> {
                            long online = t.getPlayers().stream().map(Bukkit::getPlayer).filter(p -> p != null).count();
                            scoreboard.add(online + " " + t.getColor() + t.getFriendlyName());
                        });
                    }
                }
                scoreboard.add(" ");
                // TODO: Reimplement endgame display
//                if (arena.nextEndgamePhase == null && (arena.currentEndgamePhase == UHCArena.EndgamePhase.NORMALGAME || arena.currentEndgamePhase == UHCArena.EndgamePhase.SHRINKING_WORLDBORDER)) {
                if (arena.getWorld().getWorldBorder().getSize() > arena.getProperties().WORLDBORDER_END_SIZE.get()) {
                    scoreboard.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "World Border:");
                    double[] wbPos = arena.worldborderLoc();
                    scoreboard.add("from -" + UtilTime.trim(1, wbPos[0]) + " to +" + UtilTime.trim(1, wbPos[0]));
                    scoreboard.add(" ");
                }
                if (arena.getEndgameHandler().getNextEndgamePhaseOn() != -1) {
                    scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "" +
                            ((arena.getEndgameHandler().getNextEndgamePhase() == EndgameHandler.EndgamePhase.UNKNOWN) ? arena.getEndgameHandler().getCurrentEndgamePhase().getName() :
                                    arena.getEndgameHandler().getNextEndgamePhase().getName()));
                    if (arena.getEndgameHandler().getNextEndgamePhase() == EndgameHandler.EndgamePhase.UNKNOWN) {
                        scoreboard.add("   ACTIVE");
                    } else {
                        scoreboard.add("  in " + UtilTime.format(1, arena.getEndgameHandler().getNextEndgamePhaseOn() - System.currentTimeMillis(), UtilTime.TimeUnit.FIT));
                    }
                    scoreboard.add(" ");
                }

               /* } else if (arena.nextEndgamePhase != null && arena.nextEndgamePhaseIn != -1) {
                    scoreboard.add(ChatColor.DARK_RED + "" + ChatColor.BOLD + arena.nextEndgamePhase.getName());
                    if (System.currentTimeMillis() < arena.nextEndgamePhaseIn) {
                        scoreboard.add(" in " + UtilTime.format(1, arena.nextEndgamePhaseIn - System.currentTimeMillis(), UtilTime.TimeUnit.FIT));
                    }
                    scoreboard.add(" ");
                }*/
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
