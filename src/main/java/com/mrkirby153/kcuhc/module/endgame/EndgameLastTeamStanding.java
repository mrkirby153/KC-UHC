package com.mrkirby153.kcuhc.module.endgame;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.msc.prestige.PrestigeModule;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EndgameLastTeamStanding extends UHCModule {

    private UHCGame game;

    @Inject
    public EndgameLastTeamStanding(UHCGame game) {
        super("Last Team Standing", "The game will automatically end when there is one team left.",
            Material.IRON_SWORD);
        this.game = game;
        this.autoLoad = true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.FAST && game.getCurrentState() == GameState.ALIVE) {
            List<UHCTeam> aliveTeams = new ArrayList<>();
            game.getTeams().values().forEach(team -> {
                if (team.getPlayers().size() > 0) {
                    aliveTeams.add(team);
                }
            });

            if (aliveTeams.size() <= 1) {
                if (aliveTeams.size() == 0) {
                    game.stop("????", Color.GREEN);
                    return;
                }
                // Update the prestige count if it's loaded
                ModuleRegistry.INSTANCE.getLoadedModule(PrestigeModule.class).ifPresent(mod -> {
                    HashMap<UUID, Integer> map = mod.getPrestigeMap();
                    aliveTeams.get(0).getPlayers().forEach(p -> {
                        map.put(p, map.getOrDefault(p, 0) + 1);
                    });
                });
                game.stop(aliveTeams.get(0).getTeamName(), aliveTeams.get(0).toColor());
            }
        }
    }
}
