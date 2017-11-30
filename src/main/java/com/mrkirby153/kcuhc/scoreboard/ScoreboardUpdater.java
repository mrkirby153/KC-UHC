package com.mrkirby153.kcuhc.scoreboard;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.player.PvPGraceModule;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.scoreboard.items.ElementHeadedText;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * The scoreboard updater
 */
public class ScoreboardUpdater implements Listener {

    private final UHCScoreboard scoreboard;
    private final UHCGame game;
    private final UHC uhc;


    @Inject
    public ScoreboardUpdater(UHC plugin, UHCGame game) {
        this.scoreboard = new UHCScoreboard(plugin);
        this.game = game;
        this.uhc = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.FAST) {
            return;
        }
        // Update scoreboard
        Bukkit.getOnlinePlayers().stream().filter(p -> p.getScoreboard() != scoreboard.getBoard())
            .forEach(p -> p.setScoreboard(scoreboard.getBoard()));
        scoreboard.reset();
        switch (this.game.getCurrentState()) {
            case COUNTDOWN:
            case WAITING:
                scoreboard.add(" ");
                if (game.getCurrentState() == GameState.COUNTDOWN) {
                    scoreboard.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Starting...");
                } else {
                    scoreboard.add(ChatColor.RED + "" + ChatColor.BOLD + "Waiting for start...");
                }
                scoreboard.add(" ");
                scoreboard
                    .add("Players Online: " + ChatColor.GOLD + Bukkit.getOnlinePlayers().size());
                scoreboard.add(" ");
                scoreboard.add(" ");
                break;
            case ALIVE:
                ModuleRegistry.INSTANCE.getLoadedModule(PvPGraceModule.class).ifPresent(mod -> {
                    if (mod.getGraceTimeRemaining() > 0) {
                        scoreboard.add(" ");
                        scoreboard.add(new ElementHeadedText(
                            ChatColor.AQUA + "" + ChatColor.BOLD + "PvP Enabled in", Time.INSTANCE
                            .format(1, mod.getGraceTimeRemaining(), Time.TimeUnit.FIT)));
                    }
                });
                scoreboard.add(" ");
                // 9 slots for teams/players/etc.
                List<UHCTeam> aliveTeams = new ArrayList<>();
                game.getTeams().values().forEach(t -> {
                    if (t.getPlayers().size() > 0) {
                        aliveTeams.add(t);
                    }
                });
                scoreboard
                    .add(ChatColor.GREEN + "Teams Alive: " + ChatColor.WHITE + aliveTeams.size());
                scoreboard.add(" ");
                ModuleRegistry.INSTANCE.getLoadedModule(WorldBorderModule.class)
                    .ifPresent(worldBorderModule -> {
                        scoreboard.add(new ElementHeadedText(
                            ChatColor.YELLOW + "" + ChatColor.BOLD + "World Border",
                            String.format("from -%.1f to +%.1f",
                                worldBorderModule.worldborderLoc()[0],
                                worldBorderModule.worldborderLoc()[0])));
                    });
                scoreboard.add(
                    new ElementHeadedText(ChatColor.GREEN + "" + ChatColor.BOLD + "Time Elapsed",
                        Time.INSTANCE.format(1, System.currentTimeMillis() - game.getStartTime(),
                            Time.TimeUnit.FIT)));

        }
        scoreboard.draw();
    }
}
