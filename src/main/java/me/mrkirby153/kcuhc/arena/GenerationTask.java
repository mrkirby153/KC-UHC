package me.mrkirby153.kcuhc.arena;

import me.mrkirby153.kcuhc.UHC;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Field;
import java.util.List;

public class GenerationTask implements Runnable {
    private final UHC plugin;
    private final UHCArena arena;
    private final World world;
    private final int minX;
    private final int minZ;
    private int currentX;
    private int currentZ;
    private final int maxX;
    private final int maxZ;
    private boolean pausing = false;
    private int delay = 0;
    private final boolean wasSpawnLoaded;
    private boolean waitingForChunks = false;
    private int lastChunkCount = 0;

    private boolean forceSaveUsable = true;

    private long nextTime;

    public GenerationTask(UHC plugin, UHCArena arena, World world, int minX, int minZ, int maxX, int maxZ) {
        this.plugin = plugin;
        this.arena = arena;
        this.world = world;
        this.minX = (minX * 32);
        this.minZ = (minZ * 32);
        this.maxX = (maxX * 32 + 32);
        this.maxZ = (maxZ * 32 + 32);
        this.currentX = (minX * 32);
        this.currentZ = (minZ * 32);
        this.wasSpawnLoaded = world.getKeepSpawnInMemory();
        world.setKeepSpawnInMemory(false);
        forceSave(true);
        nextTime = System.currentTimeMillis();
    }

    public void run() {
        int unsavedChunks = getUnsavedChunks();
        if (unsavedChunks > 3000)
            this.waitingForChunks = true;
        if ((this.waitingForChunks) && (unsavedChunks < 50)) {
            this.waitingForChunks = false;
            System.gc();
        }
        if (this.waitingForChunks) {
            if (unsavedChunks != this.lastChunkCount)
                this.plugin.getLogger().info("Unsaved chunks: " + unsavedChunks);
            this.lastChunkCount = unsavedChunks;
            return;
        }
        if (this.pausing) {
            if (Runtime.getRuntime().freeMemory() > 20971520L) {
                if (this.delay-- <= 0) {
                    this.pausing = false;
                    this.plugin.getLogger().info("Enough memory is available to continue.");
                }
            } else {
                this.delay = 20;
            }
        } else if (Runtime.getRuntime().freeMemory() < 10485760L) {
            this.pausing = true;
            this.delay = 20;
            this.plugin.getLogger().severe("Less than 10MB of memory is available. Pausing until memory is freed.");
            return;
        }
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                this.world.loadChunk(this.currentX + x, this.currentZ + z);
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                this.world.unloadChunk(this.currentX + x, this.currentZ + z, true, false);
        this.currentX += 1;
        if (this.currentX >= this.maxX) {
            this.currentX = this.minX;
            this.currentZ += 1;
            if (this.currentZ >= this.maxZ) {
                if (this.wasSpawnLoaded)
                    this.world.setKeepSpawnInMemory(true);
                forceSave(false);
                this.arena.generationComplete();
            }
        }
        if (System.currentTimeMillis() > nextTime) {
            nextTime = System.currentTimeMillis() + 10000;
            Bukkit.broadcastMessage("Generation " + ChatColor.RED + ChatColor.BOLD + String.format("%.2f", getPercentDone()) + "% " + ChatColor.RESET + "complete...");
        }
    }

    private void forceSave(boolean enabled) {
        if (!this.forceSaveUsable)
            return;
        try {
            Class clazz = Class.forName("net.minecraft.server.FileIOThread");
            Object fileIOThread = clazz.getDeclaredField("a").get(null);
            Field force = clazz.getDeclaredField("e");
            force.setAccessible(true);
            force.setBoolean(fileIOThread, enabled);
        } catch (Exception ex) {
            this.forceSaveUsable = false;
        }
    }

    private int getUnsavedChunks() {
        try {
            Class clazz = Class.forName("net.minecraft.server.FileIOThread");
            Object fileIOThread = clazz.getDeclaredField("a").get(null);
            Field chunkSaverField = clazz.getDeclaredField("b");
            chunkSaverField.setAccessible(true);
            Object chunkSaver = ((List) chunkSaverField.get(fileIOThread)).get(0);
            Class clazz2 = chunkSaver.getClass();
            Field chunkField = clazz2.getDeclaredField("a");
            chunkField.setAccessible(true);
            return ((List) chunkField.get(chunkSaver)).size();
        } catch (Exception ex) {
        }
        return 0;
    }

    public int getTicksLeft() {
        if (this.pausing)
            return -1;
        return ((this.maxZ - this.currentZ - 1) * (this.maxX - this.minX) + (this.maxX - this.currentX)) * 2;
    }

    public double getPercentDone() {
        return 100.0D - ((this.maxZ - this.currentZ - 1) * (this.maxX - this.minX) + (this.maxX - this.currentX)) * 100.0D / ((this.maxZ - this.minZ) * (this.maxX - this.minX));
    }
}