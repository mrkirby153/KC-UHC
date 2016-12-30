package me.mrkirby153.kcuhc.world;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.Module;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.*;
import java.util.HashSet;
import java.util.Random;


public class MultiWorldHandler extends Module<UHC> implements Listener {

    private static final Random random = new Random();

    private HashSet<String> worldsQueuedForDelete = new HashSet<>();

    public MultiWorldHandler(UHC plugin) {
        super("Multi World", "1.0", plugin);
    }

    @Override
    protected void init() {
        registerListener(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        World.Environment from = event.getFrom().getWorld().getEnvironment();
        World.Environment to = World.Environment.NORMAL;

        PortalType type = PortalType.ENDER;

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            type = PortalType.NETHER;
            event.useTravelAgent(true);
        }

        if (event.getTo() == null) {
            if (from == World.Environment.NETHER) {
                if (type == PortalType.NETHER) {
                    to = World.Environment.NORMAL;
                }
            }
            if (from == World.Environment.NORMAL) {
                if (type == PortalType.NETHER) {
                    to = World.Environment.NETHER;
                }
                if (type == PortalType.ENDER)
                    to = World.Environment.THE_END;
            }
        }
        Location loc = event.getFrom().clone();
        if (from == World.Environment.NETHER) {
            loc.setWorld(Bukkit.getWorld(loc.getWorld().getName().replace("_nether", "")));
        }

        if (from == World.Environment.NORMAL) {
            if (type == PortalType.NETHER)
                loc.setWorld(Bukkit.getWorld(loc.getWorld().getName() + "_nether"));
            if (type == PortalType.ENDER) {
                loc.setX(100);
                loc.setZ(0);
                loc.setY(50);
                loc.setWorld(Bukkit.getWorld(loc.getWorld().getName() + "_the_end"));
                if (loc.getWorld() == null)
                    return;
                event.setTo(loc);
                Block block = loc.getBlock();
                for (int x = block.getX() - 2; x <= block.getX() + 2; x++) {
                    for (int z = block.getZ() - 2; z <= block.getZ() + 2; z++) {
                        Block platformBlock = loc.getWorld().getBlockAt(x, block.getY() - 1, z);
                        if (platformBlock.getType() != Material.OBSIDIAN) {
                            platformBlock.setType(Material.OBSIDIAN);
                        }
                        for (int yMod = 1; yMod <= 3; yMod++) {
                            Block b = platformBlock.getRelative(BlockFace.UP, yMod);
                            if (b.getType() != Material.AIR) {
                                b.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }

        if (from == World.Environment.THE_END) {
            loc.setWorld(Bukkit.getWorld(loc.getWorld().getName().replace("_the_end", "")));
        }

        if (type == PortalType.NETHER) {
            event.getPortalTravelAgent().setCanCreatePortal(true);
            event.setTo(event.getPortalTravelAgent().findOrCreate(scale(loc, to, from, event.getFrom().getWorld().getMaxHeight(), loc.getWorld().getMaxHeight())));
        }

        if (type == PortalType.ENDER) {
            if (from == World.Environment.THE_END) {
                if (event.getPlayer().getBedSpawnLocation() != null && event.getPlayer().getBedSpawnLocation().getWorld().getUID() == loc.getWorld().getUID())
                    event.getPlayer().teleport(event.getPlayer().getBedSpawnLocation());
                else
                    event.getPlayer().teleport(loc.getWorld().getSpawnLocation());
            }
            if (to == World.Environment.THE_END) {
                event.setTo(loc);
            }
        }
    }

    public WorldStatus getStatus(String world) {
        if (world == null)
            return null;
        world = world.replace("_the_end", "").replace("_nether", "");
        File statusFile = new File(Bukkit.getWorldContainer(), world + "/status.dat");
        if (!statusFile.exists()) {
            setStatus(world, WorldStatus.CREATED);
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(statusFile));
            WorldStatus worldStatus = WorldStatus.valueOf(reader.readLine().toUpperCase());
            reader.close();
            return worldStatus;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setStatus(World world, WorldStatus status) {
        setStatus(world.getName(), status);
    }

    public WorldStatus getStatus(World world) {
        return getStatus(world.getName());
    }

    public World createWorld() {
        String worldName = "UHC_" + random.nextInt(999);
        while (getStatus(worldName) != null)
            worldName = "UHC_" + random.nextInt(999);
        getPlugin().uhcWorld = Bukkit.createWorld(new WorldCreator(worldName));
        getPlugin().uhcWorld_nether = Bukkit.createWorld(new WorldCreator(worldName+"_nether").environment(World.Environment.NETHER));
        getPlugin().uhcWorld_end = Bukkit.createWorld(new WorldCreator(worldName+"_the_end").environment(World.Environment.THE_END));
        setStatus(getPlugin().uhcWorld, WorldStatus.CREATED);
        return getPlugin().uhcWorld;
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        if (worldsQueuedForDelete.remove(event.getWorld().getName())) {
           getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), ()-> deleteWorld(event.getWorld().getName()), 20L);
        }
    }

    public void deleteUHCWorld(String name) {
        worldsQueuedForDelete.add(name);
        worldsQueuedForDelete.add(name + "_nether");
        worldsQueuedForDelete.add(name + "_the_end");

        Bukkit.unloadWorld(name, true);
        Bukkit.unloadWorld(name + "_nether", true);
        Bukkit.unloadWorld(name + "_the_end", true);
    }

    private void deleteWorld(String world) {
        log("Deleting world " + world);
        worldsQueuedForDelete.add(world);
        File toDelete = new File(Bukkit.getWorldContainer(), world);
        try {
            FileUtils.deleteDirectory(toDelete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setWorld(String id) {
        getPlugin().uhcWorld = Bukkit.getWorld("UHC_" + id);
        getPlugin().uhcWorld_nether = Bukkit.getWorld("UHC_" + id + "_nether");
        getPlugin().uhcWorld_end = Bukkit.getWorld("UHC_" + id + "_the_end");
    }

    public void setStatus(String world, WorldStatus status) {
        if (world == null)
            return;
        world = world.replace("_the_end", "").replace("_nether", "");
        try {
            File statusFile = new File(Bukkit.getWorldContainer(), world + "/status.dat");
            if (!statusFile.exists())
                statusFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(statusFile));
            writer.write(status.toString() + "\n");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Location scale(Location location, World.Environment from, World.Environment to, double fromMaxHeight, double toMaxHeight) {
        Location newLoc = location.clone();
        double fromScaling = 1;
        double toScaling = 1;
        if (from == World.Environment.NETHER) {
            fromScaling = 8;
        }
        if (to == World.Environment.NETHER) {
            toScaling = 8;
        }
        double scale = 1D * (toScaling / fromScaling);
        newLoc.setX(newLoc.getX() * scale);
        newLoc.setZ(newLoc.getZ() * scale);
        newLoc.setY(newLoc.getY() * (toMaxHeight / fromMaxHeight));
        return newLoc;
    }
}
