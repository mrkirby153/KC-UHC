package me.mrkirby153.kcuhc.module.player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.PacketConstructor;
import com.google.common.base.Throwables;
import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.SpawnUtils;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.team.LoneWolfTeam;
import me.mrkirby153.kcuhc.team.TeamSpectator;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SpreadPlayersModule extends UHCModule {

    public SpreadPlayersModule() {
        super(Material.ENDER_PEARL, 0, "Spread Players", true, "Distribute players randomly through the map");
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("Spreading players!"));
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("No longer spreading players!"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == UHCArena.State.RUNNING)
            distributeTeams();
    }

    public void distributeTeams() {
        this.distributeTeams(getPlugin().arena.getProperties().WORLDBORDER_START_SIZE.get());
    }

    public void distributeTeams(int minRadius) {
        UHCArena arena = getPlugin().arena;
        System.out.println("Worldborder location: +-" + arena.getWorld().getWorldBorder().getSize() / 2);
        System.out.println("Spreading teams...");

        // Calculate team spawn locations
        Map<UHCTeam, Location> teamSpawnLocations = new HashMap<>();
        List<Location> finalSpawnLocs = new ArrayList<>();
        for (UHCTeam team : getPlugin().teamHandler.teams()) {
            if (team instanceof LoneWolfTeam || team instanceof TeamSpectator)
                continue;
            Location randomSpawn = SpawnUtils.getRandomSpawn(arena.getWorld(), arena.getProperties().WORLDBORDER_START_SIZE.get());
            teamSpawnLocations.put(team, randomSpawn);
        }

        Map<UUID, Location> loneWolfSpawnLocations = new HashMap<>();
        ModuleRegistry.getLoadedModule(LoneWolfModule.class).ifPresent(loneWolfModule -> loneWolfModule.getLoneWolves().forEach(e -> loneWolfSpawnLocations.put(e, SpawnUtils.getRandomSpawn(arena.getWorld(), arena.getProperties().WORLDBORDER_START_SIZE.get()))));

        // Verify that the teams are spread far enough apart
        for (Map.Entry<UHCTeam, Location> entry : teamSpawnLocations.entrySet()) {
            Location spawnLoc = entry.getValue();
            UHCTeam team = entry.getKey();
            List<Location> locs = new ArrayList<>(teamSpawnLocations.values());
            locs.addAll(loneWolfSpawnLocations.values());
            locs.addAll(finalSpawnLocs);
            for (int i = 0; i < 1000; i++) {
                boolean clash = false;
                for (Location otherLoc : locs) {
                    if (otherLoc.distanceSquared(spawnLoc) < Math.pow(minRadius, 2) && !otherLoc.equals(spawnLoc)) {
                        System.out.println("CLASH: " + spawnLoc.toString() + " is too close to " + otherLoc.toString());
                        clash = true;
                        break;
                    }
                }
                if (!clash) {
                    break;
                }
                spawnLoc = SpawnUtils.getRandomSpawn(arena.getWorld(), arena.getProperties().WORLDBORDER_START_SIZE.get());
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

        // Spread the lone wolves
        if(ModuleRegistry.isLoaded(LoneWolfModule.class)) {
            System.out.println("Spreading Lone Wolves...");
            for (Map.Entry<UUID, Location> entry : loneWolfSpawnLocations.entrySet()) {
                Location spawnLoc = entry.getValue();
                List<Location> locs = new ArrayList<>(finalSpawnLocs);
                locs.addAll(loneWolfSpawnLocations.values());
                for (int i = 0; i < 1000; i++) {
                    boolean clash = false;
                    for (Location otherLoc : locs) {
                        if (otherLoc.distanceSquared(spawnLoc) < Math.pow(minRadius, 2) && !otherLoc.equals(spawnLoc)) {
                            System.out.println("CLASH: " + spawnLoc.toString() + " is too close to " + otherLoc.toString());
                            clash = true;
                            break;
                        }
                    }
                    if (!clash) {
                        break;
                    }
                    spawnLoc = SpawnUtils.getRandomSpawn(arena.getWorld(), arena.getProperties().WORLDBORDER_START_SIZE.get());
                }
                finalSpawnLocs.add(spawnLoc);
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null)
                    continue;
                player.teleport(spawnLoc);
                System.out.println(String.format("\tTeleporting %s to %.2f %.2f %.2f", player.getName(), spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));
            }
        }

        System.out.println("Spread teams!");
        System.out.println("Despawning players...");
        // Despawn all the players in attempt to prevent invisible players
        for (Player p : Bukkit.getOnlinePlayers()) {
            PacketContainer destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getModifier().writeDefaults();
            destroyPacket.getIntegerArrays().write(0, new int[]{p.getEntityId()});
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(p.getUniqueId()))
                    continue;
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(other, destroyPacket);
                } catch (InvocationTargetException e) {
                    Throwables.propagate(e);
                }
            }
        }

        // Respawn everyone 5 ticks later
        Bukkit.getServer().getScheduler().runTaskLater(getPlugin(), () -> {
            System.out.println("Respawning players");
            for (Player p : Bukkit.getOnlinePlayers()) {
                PacketConstructor spawnPacket = ProtocolLibrary.getProtocolManager().createPacketConstructor(PacketType.Play.Server.NAMED_ENTITY_SPAWN, p);
                PacketContainer container = spawnPacket.createPacket(p);

                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (p.getUniqueId().equals(other.getUniqueId()))
                        continue;
                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(other, container);
                    } catch (InvocationTargetException e) {
                        Throwables.propagate(e);
                    }
                }
            }
        }, 5L);
    }
}
