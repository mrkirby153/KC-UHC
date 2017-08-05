package com.mrkirby153.kcuhc.module.msc.cornucopia;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.SpawnUtils;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.structure.Structure;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;

import java.io.File;
import java.util.Random;

public class CornucopiaModule extends UHCModule {

    private static final int CORN_SIZE_X = 6;
    private static final int CORN_SIZE_Y = 6;
    private static final int CORN_SIZE_Z = 6;
    private UHC uhc;
    private boolean spawned = false;

    @Inject
    public CornucopiaModule(UHC uhc) {
        super("Spawn Cornucopia", "Spawns a cornucopia in a random location", Material.CHEST);
        this.uhc = uhc;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE)
            spawned = false;
    }

    @Override
    public void onLoad() {
        // Copy the cornucopia file out of the jar
        if (!new File(uhc.getDataFolder(), "cornucopia.schematic").exists())
            uhc.saveResource("cornucopia.schematic", false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.FAST && uhc.getGame().getCurrentState() == GameState.ALIVE) {
            // Spawn the cornucopia when the border travel is 75% complete
            ModuleRegistry.INSTANCE.getLoadedModule(WorldBorderModule.class).ifPresent(worldBorderModule -> {
                double currentSize = UHC.getUHCWorld().getWorldBorder().getSize();

                double startSize = worldBorderModule.getStartSize();
                double endSize = worldBorderModule.getEndSize();

                double blocksTraveled = startSize - currentSize;
                double totalBlocksToTravel = startSize - endSize;

                double percentDone = blocksTraveled / totalBlocksToTravel;

                if (percentDone >= 0.75 && !spawned) {
                    spawnCornucopia(currentSize);
                    spawned = true;
                }
            });
        }
    }

    public void spawnCornucopia(double maxRadius) {
        Location randomSpawn = SpawnUtils.getRandomSpawn(UHC.getUHCWorld(), (int) Math.floor(maxRadius));
        System.out.println("Cornucopia has spawned at " + randomSpawn);
        new Structure(new File(uhc.getDataFolder(), "cornucopia.schematic")).place(randomSpawn);
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, SoundCategory.MASTER, 1F, 1F);
            player.sendMessage(C.m("Game", "A cornucopia has spawned at ({x}, {y}, {z})",
                    "{x}", randomSpawn.getBlockX(), "{y}", randomSpawn.getBlockY(), "{z}", randomSpawn.getBlockZ()).toLegacyText());
        });
        fillChests(randomSpawn);
    }

    private void fillChests(Location location) {
        CornucopiaLootTable table = new CornucopiaLootTable();
        Random random = new Random();
        for (int x = location.getBlockX(); x >= location.getBlockX() - CORN_SIZE_X; x--) {
            for (int z = location.getBlockZ(); z < location.getBlockZ() + CORN_SIZE_Z; z++) {
                for (int y = location.getBlockY(); y < location.getBlockY() + CORN_SIZE_Y; y++) {
                    Block b = location.getWorld().getBlockAt(x, y, z);
                    if (b.getType() == Material.CHEST) {
                        Chest chest = (Chest) b.getState();
                        table.get(5).forEach(item -> {
                            int slot = 0;
                            do {
                                slot = random.nextInt(chest.getBlockInventory().getSize());
                            } while (chest.getBlockInventory().getItem(slot) != null);
                            chest.getBlockInventory().setItem(slot, item);
                        });
                    }
                }
            }
        }
    }
}
