package com.mrkirby153.kcuhc.game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Random;

public class SpawnUtils {

    private static Random random = new Random();

    public static Block getHighest(World world, int x, int z) {
        return world.getHighestBlockAt(x, z);
    }

    public static Location getRandomSpawn(World world, int borderSize) {
        borderSize /= 2; // Divide by 2 to get the radius
        Location loc = null;
//        borderSize *= 0.75; // Shrink the size by 75% to prevent spawning outside the border
        while (loc == null) {
            Block block;
            int x = random.nextInt(borderSize * 2) - borderSize;
            int z = random.nextInt(borderSize * 2) - borderSize;
            block = getHighest(world, x, z);

            // Prevent spawning in lava or water (That would be embarrassing)
//            if (!isValid(block)) {
//                continue;
//            }
            loc = block.getLocation().add(0.5, 0.5, 0.5);
        }
        return loc;
    }

    public static Location getSpawnAround(Location location, int radius) {
        Location loc = null;
        while (loc == null) {
            Block block;

            int x = location.getBlockX();
            int z = location.getBlockZ();
            int xOffset = random.nextInt(radius * 2) - radius;
            int zOffset = random.nextInt(radius * 2) - radius;

            block = getHighest(location.getWorld(), x + xOffset, z + zOffset);
//            if (!isValid(block)) {
//                continue;
//            }
            loc = block.getLocation().add(0.5, 0.5, 0.5);
        }
        return loc;
    }

    private static boolean isValid(Block block) {
        return !block.getRelative(BlockFace.DOWN).isLiquid()
            && block.getType() == Material.AIR
            && block.getRelative(BlockFace.UP).getType() == Material.AIR;
    }
}