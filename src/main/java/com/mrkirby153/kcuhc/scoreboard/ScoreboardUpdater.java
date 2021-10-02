package com.mrkirby153.kcuhc.scoreboard;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.UHCGame;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Objective;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The scoreboard updater
 */
public class ScoreboardUpdater implements Listener {

    private final UHCGame game;
    private final UHC uhc;

    private final Map<UUID, UHCScoreboard> scoreboardMap = new HashMap<>();


    @Inject
    public ScoreboardUpdater(UHC plugin, UHCGame game) {
        this.game = game;
        this.uhc = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.FAST) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(this::drawScoreboard);
    }

    private void drawScoreboard(Player player) {
        UHCScoreboard scoreboard = this.scoreboardMap.get(player.getUniqueId());
        if (scoreboard == null) {
            this.scoreboardMap
                .put(player.getUniqueId(), scoreboard = new UHCScoreboard(this.uhc));
        }
        scoreboard.reset();
        UHCScoreboard finalScoreboard = scoreboard;
        List<ScoreboardModule> modulesToRender = ScoreboardModuleManager.INSTANCE.getModules().stream()
            .filter(ScoreboardModule::shouldDisplay).collect(Collectors.toList());
        if (modulesToRender.size() > 0) {
            scoreboard.addSpacer();
        }
        modulesToRender.forEach(mod -> {
            mod.drawScoreboard(finalScoreboard, player);
            finalScoreboard.addSpacer();
        });
        // Update the health objectives
        Objective tabList = scoreboard.getTablistHealth();
        Objective belowName = scoreboard.getBelowNameHealth();
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (tabList.getScore(p.getName()).getScore() == 0 && !p.isDead()) {
                tabList.getScore(p.getName()).setScore((int) p.getHealth());
            }
            belowName.getScore(p.getName()).setScore((int) (p.getHealth() + getAbsorption(p)));
        });
        scoreboard.draw();
        if (player.getScoreboard() != scoreboard.getBoard()) {
            player.setScoreboard(scoreboard.getBoard());
        }
    }

    private double getAbsorption(Player player) {
        return player.getAbsorptionAmount();
    }
}
