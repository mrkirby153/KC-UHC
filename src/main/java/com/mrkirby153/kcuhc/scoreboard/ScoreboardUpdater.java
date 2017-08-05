package com.mrkirby153.kcuhc.scoreboard;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.scoreboard.KirbyScoreboard;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * The scoreboard updater
 */
public class ScoreboardUpdater implements Listener {

    private final KirbyScoreboard scoreboard;
    private final UHC plugin;
    private final UHCGame game;

    public ScoreboardUpdater(UHC plugin) {
        this.plugin = plugin;
        this.scoreboard = new UHCScoreboard(plugin);
        this.game = plugin.getGame();
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.FAST)
            return;
        // Update scoreboard
        Bukkit.getOnlinePlayers().stream().filter(p -> p.getScoreboard() != scoreboard.getBoard())
                .forEach(p -> p.setScoreboard(scoreboard.getBoard()));
        scoreboard.reset();
        switch (this.game.getCurrentState()) {
            case COUNTDOWN:
            case WAITING:
                scoreboard.add(" ");
                if (game.getCurrentState() == GameState.COUNTDOWN)
                    scoreboard.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Starting...");
                else
                    scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "Waiting for start...");
                scoreboard.add(" ");
                scoreboard.add("Players Online: " + ChatColor.GOLD + Bukkit.getOnlinePlayers().size());
                scoreboard.add(" ");
                scoreboard.add(" ");
                break;
            case ALIVE:
                scoreboard.add(" ");
                // 9 slots for teams/players/etc.
                List<UHCTeam> aliveTeams = new ArrayList<>();
                game.getTeams().values().forEach(t -> {
                    if (t.getPlayers().size() > 0)
                        aliveTeams.add(t);
                });
                List<Player> alivePlayers = new ArrayList<>();
                // Players not on the spectators team
                Bukkit.getServer().getOnlinePlayers().stream().filter(Player::isValid).filter(p -> {
                    ScoreboardTeam team = game.getTeam(p);
                    return team == null || !(team instanceof SpectatorTeam);
                }).forEach(alivePlayers::add);
                if (alivePlayers.size() < 9) {
                    // Display players sorted by health
                    alivePlayers.sort((o1, o2) -> (int) (o2.getHealth() - o1.getHealth()));
                    alivePlayers.forEach(p -> {
                        UHCTeam team = (UHCTeam) game.getTeam(p);
                        ChatColor color = team != null ? team.getColor() : ChatColor.WHITE;
                        scoreboard.add(color + p.getName() + " " + ChatColor.RED + (int) p.getHealth() + " " + Character.toString('\u2764'));
                    });

                } else if (aliveTeams.size() < 9) {
                    // Display alive teams sorted by players
                    aliveTeams.sort((o1, o2) -> o2.getPlayers().size() - o1.getPlayers().size());
                    aliveTeams.forEach(t -> scoreboard.add(t.getColor() + t.getTeamName() + ChatColor.RED + " - " + t.getPlayers().size() + ((t.getPlayers().size() == 1) ? " player" : " players")));
                } else {
                    // Display alive team count
                    scoreboard.add(new ElementHeadedText(ChatColor.AQUA + "" + ChatColor.BOLD + "Teams",
                            String.format(ChatColor.GOLD + "%s" + ChatColor.WHITE + " alive", aliveTeams.size())));
                }
                scoreboard.add(" ");
                ModuleRegistry.INSTANCE.getLoadedModule(WorldBorderModule.class).ifPresent(worldBorderModule -> {
                    scoreboard.add(new ElementHeadedText(ChatColor.YELLOW + "" + ChatColor.BOLD + "World Border",
                            String.format("from -%.1f to +%.1f", worldBorderModule.worldborderLoc()[0], worldBorderModule.worldborderLoc()[0])));
                });
                scoreboard.add(new ElementHeadedText(ChatColor.GREEN + "" + ChatColor.BOLD + "Time Elapsed",
                        Time.format(1, System.currentTimeMillis() - game.getStartTime(), Time.TimeUnit.FIT)));

        }
        scoreboard.draw();
    }
}
