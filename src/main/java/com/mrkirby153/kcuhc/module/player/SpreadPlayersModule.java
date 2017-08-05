package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.SpawnUtils;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.ModuleUnloadEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpreadPlayersModule extends UHCModule {

    private UHCGame game;

    private WorldBorderModule module;

    private int minDistance = 50;

    @Inject
    public SpreadPlayersModule(UHCGame game) {
        super("Spread Players", "Distribute players randomly throughout the map", Material.ENDER_PEARL);
        this.game = game;
        this.autoLoad = true;
    }

    /**
     * Distribute teams in the minimum radius
     *
     * @param minRadius The radius
     */
    public void distributeTeams(int minRadius) {
        System.out.println("Worldborder location +- " + UHC.getUHCWorld().getWorldBorder().getSize() / 2);
        System.out.println("Spreading teams...");

        Map<UHCTeam, Location> spawnLocations = new HashMap<>();
        List<Location> finalSpawnLocs = new ArrayList<>();
        for (UHCTeam team : game.getTeams().values()) {
            Location randomSpawn = SpawnUtils.getRandomSpawn(UHC.getUHCWorld(), module.getStartSize());
            spawnLocations.put(team, randomSpawn);
        }
        for (Map.Entry<UHCTeam, Location> entry : spawnLocations.entrySet()) {
            Location spawnLoc = entry.getValue();
            UHCTeam team = entry.getKey();
            List<Location> locs = new ArrayList<>(spawnLocations.values());
            locs.addAll(finalSpawnLocs);
            for (int i = 0; i < 1000; i++) {
                boolean clash = false;
                for (Location otherLoc : locs) {
                    if (otherLoc.distanceSquared(spawnLoc) < Math.pow(minRadius, 2) && !otherLoc.equals(spawnLoc)) {
//                        System.out.println("CLASH: " + spawnLoc.toString() + " is too close to " + otherLoc.toString());
                        clash = true;
                        break;
                    }
                }
                if (!clash) {
                    break;
                }
                spawnLoc = SpawnUtils.getRandomSpawn(UHC.getUHCWorld(), module.getStartSize());
            }
            finalSpawnLocs.add(spawnLoc);
            final Location finalLoc = spawnLoc;
            System.out.println(String.format("Teleporting players on team %s around %.2f, %.2f, %.2f", team.getTeamName(), spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));
            team.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> {
                Location spawnAround = SpawnUtils.getSpawnAround(finalLoc, 2);
                System.out.println(String.format("\tTeleporting %s to %.2f, %.2f, %.2f", p.getName(), spawnAround.getX(), spawnAround.getY(), spawnAround.getZ()));
                p.teleport(spawnAround);
            });
        }
    }

    /**
     * Gets the minimum distance between players
     *
     * @return The minimum distance between players
     */
    public int getMinDistance() {
        return minDistance;
    }

    /**
     * Sets the minimum distance between teams
     *
     * @param minDistance The distance
     */
    public void setMinDistance(int minDistance) {
        this.minDistance = minDistance;
    }

    @EventHandler
    public void moduleUnload(ModuleUnloadEvent event) {
        if (event.getModule().getClass() == WorldBorderModule.class)
            event.setCancelled(true);
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE)
            this.distributeTeams(getMinDistance());
    }

    @Override
    public void onLoad() {
        if (!ModuleRegistry.INSTANCE.loaded(WorldBorderModule.class)) {
            ModuleRegistry.INSTANCE.load(module = ModuleRegistry.INSTANCE.getModule(WorldBorderModule.class));
        } else {
            module = ModuleRegistry.INSTANCE.getModule(WorldBorderModule.class);
        }
    }
}
