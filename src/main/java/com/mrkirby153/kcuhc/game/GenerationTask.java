package com.mrkirby153.kcuhc.game;

import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.Time;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GenerationTask implements Runnable {

    private static final long FREQUENCY = 1;
    public static GenerationTask instance;
    private final int minX, maxX, minZ, maxZ;
    private final World world;
    private final long startTime;
    public int generatedChunks = 0;
    public double totalChunks;
    private int x, z;
    private int taskId;
    private long nextNotifyTime = System.currentTimeMillis();
    private JavaPlugin plugin;

    private List<ChunkXZ> originalChunks = new ArrayList<>();

    private List<ChunkXZ> storedChunks = new ArrayList<>();

    private ChunkXZ lastChunk = new ChunkXZ(0, 0);

    // The original length of our leg
    private int length = -1;
    // The current size of our chunk
    private int current = 0;
    private boolean isZLeg = false;
    private boolean isNeg = false;

    private Consumer<Void> onComplete;

    public GenerationTask(JavaPlugin plugin, World world, int minX, int maxX, int minZ, int maxZ, Consumer<Void> complete) {
        this.world = world;
        this.plugin = plugin;
        this.minX = minX >> 4;
        this.minZ = minZ >> 4;

        this.maxX = maxX >> 4;
        this.maxZ = maxZ >> 4;

        this.totalChunks = (Math.abs(this.minX) + Math.abs(this.maxX)) * (Math.abs(this.maxZ) + Math.abs(this.minZ));

        x = (this.maxX + this.minX) / 2;
        z = (this.maxZ + this.minZ) / 2;


        log(String.format("Generating chunks from [%s, %s] to [%s, %s] (%s total chunks)", this.minX, this.minZ,
                this.maxX, this.maxZ, (int) this.totalChunks));
        log(String.format("Estimated time: %.2f minutes", (this.totalChunks / (20D / FREQUENCY)) / 60));

        nextNotifyTime = System.currentTimeMillis();
        startTime = System.currentTimeMillis();

        for (Chunk c : world.getLoadedChunks()) {
            originalChunks.add(new ChunkXZ(c.getX(), c.getZ()));
        }
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, FREQUENCY);
        instance = this;
        this.onComplete = complete;
    }

    public double percentDone() {
        return generatedChunks / totalChunks * 100;
    }

    @Override
    public void run() {
        // Broadcast progress every five seconds or so
        if (System.currentTimeMillis() > nextNotifyTime) {
            broadcastProgress();
            nextNotifyTime = System.currentTimeMillis() + 30000;
        }

        if (!moveToNext()) {
            return;
        }

        world.loadChunk(x, z, true);
        generatedChunks++;

        int popX = !isZLeg ? x : (x + (isNeg ? -1 : 1));
        int popZ = isZLeg ? z : (z + (!isNeg ? -1 : 1));
        world.loadChunk(popX, popZ, false);

        if (!storedChunks.contains(lastChunk) && !originalChunks.contains(lastChunk)) {
            world.loadChunk(lastChunk.x, lastChunk.z, false);
            storedChunks.add(lastChunk);
        }

        storedChunks.add(new ChunkXZ(popX, popZ));
        storedChunks.add(new ChunkXZ(x, z));

        // Flush the extra inside chunks starting oldest first
        while (storedChunks.size() > 10) {
            ChunkXZ chunk = storedChunks.remove(0);
            unloadChunk(chunk);
        }

    }

    private void broadcastProgress() {
        if (percentDone() < 100) {
            Bukkit.getServer().broadcastMessage(C.m("Pregeneration", "{percent} complete ({generated}/{total} chunks)",
                    "{percent}", String.format("%.2f", percentDone()) + "%",
                    "{generated}", generatedChunks,
                    "{total}", (int) totalChunks).toLegacyText());
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F));
        }
    }

    private void log(String message) {
        plugin.getLogger().info(String.format("[PREGENERATION] %s", message));
    }

    /**
     * Moves to the next chunk in a spiral pattern
     *
     * @return false if we're done generation
     */

    private boolean moveToNext() {
        lastChunk = new ChunkXZ(x, z);
        if (current < length) {
            current++;
        } else {
            // one side done
            current = 0;
            isZLeg ^= true;
            if (isZLeg) {
                isNeg ^= true;
                length++;
            }
        }

        if (isZLeg) {
            z += isNeg ? -1 : 1;
        } else {
            x += isNeg ? -1 : 1;
        }

        if (isZLeg && isNeg && current == 0) {
            if (x >= maxX && z >= maxZ) {
                log("Generation completed in " + Time.format(1, System.currentTimeMillis() - this.startTime, Time.TimeUnit.FIT));
                Bukkit.getScheduler().cancelTask(taskId);
                if (this.onComplete != null)
                    this.onComplete.accept(null);
                return false;
            }
        }
        return true;
    }

    /**
     * Unloads a chunk from memory, first checking if it was loaded before we started generation
     *
     * @param chunk The chunk to unload
     */
    private void unloadChunk(ChunkXZ chunk) {
        // Prevent previously loaded chunks from being unloaded
        if (originalChunks.contains(chunk))
            return;

        world.unloadChunkRequest(chunk.x, chunk.z);
    }

    private static class ChunkXZ {
        public int x;
        public int z;

        public ChunkXZ(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ChunkXZ) {
                return ((ChunkXZ) obj).x == x && ((ChunkXZ) obj).z == z;
            }
            return obj instanceof Chunk && ((Chunk) obj).getZ() == z && ((Chunk) obj).getX() == x;
        }
    }
}