package com.mrkirby153.kcuhc.module.msc.cornucopia;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.SpawnUtils;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.structure.RelativeBlock;
import me.mrkirby153.kcutils.structure.Structure;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Random;

public class CornucopiaModule extends UHCModule {

    private static final String CORN_STRUCTURE = "cornucopia.yml";

    private UHC uhc;
    private UHCGame game;
    private Structure cornucopiaStructure;

    @Inject
    public CornucopiaModule(UHC uhc, UHCGame game) {
        super("Spawn Cornucopia", "Spawns a cornucopia in a random location", Material.CHEST);
        this.uhc = uhc;
        this.game = game;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if(event.getTo() == GameState.ALIVE){
            if(this.cornucopiaStructure.getPlaced()){
                this.cornucopiaStructure.restore();
            }
        }
        if(event.getTo() == GameState.ENDED){
            this.cornucopiaStructure.restore();
        }
    }

    @Override
    public void onLoad() {
        // Copy the cornucopia file out of the jar
        File cornucopiaFile = new File(uhc.getDataFolder(), CORN_STRUCTURE);
        if (!cornucopiaFile.exists()) {
            this.uhc.getLogger()
                .info("[CORNUCOPIA] Cornucopia structure file does not exist, creating...");
            this.uhc.saveResource(CORN_STRUCTURE, false);
        }
        // Load the cornucopia structure
        this.cornucopiaStructure = new Structure(
            YamlConfiguration.loadConfiguration(cornucopiaFile));
        this.uhc.getLogger()
            .info(String.format("[CORNUCOPIA] Loaded cornucopia with size %s, %s, %s",
                this.cornucopiaStructure.getSizeX(), this.cornucopiaStructure.getSizeY(),
                this.cornucopiaStructure.getSizeZ()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.FAST
            && uhc.getGame().getCurrentState() == GameState.ALIVE) {
            // Spawn the cornucopia when the border travel is 75% complete
            ModuleRegistry.INSTANCE.getLoadedModule(WorldBorderModule.class)
                .ifPresent(worldBorderModule -> {
                    double currentSize = this.game.getUHCWorld().getWorldBorder().getSize();

                    double startSize = worldBorderModule.getStartSize();
                    double endSize = worldBorderModule.getEndSize();

                    double blocksTraveled = startSize - currentSize;
                    double totalBlocksToTravel = startSize - endSize;

                    double percentDone = blocksTraveled / totalBlocksToTravel;

                    if (percentDone >= 0.75 && !this.cornucopiaStructure.getPlaced()) {
                        spawnCornucopia(currentSize);
                    }
                });
        }
    }

    public void spawnCornucopia(double maxRadius) {
        Location randomSpawn = SpawnUtils
            .getRandomSpawn(this.game.getUHCWorld(), (int) Math.floor(maxRadius * 0.75)).subtract(new Vector(0, 1, 0));
        System.out.println("Cornucopia has spawned at " + randomSpawn);
        this.cornucopiaStructure.restore();
        this.cornucopiaStructure.placeAll(randomSpawn);
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, SoundCategory.MASTER, 1F,
                1F);
            player.sendMessage(
                Chat.message("Game", "A cornucopia has spawned at ({x}, {y}, {z})",
                    "{x}", randomSpawn.getBlockX(), "{y}", randomSpawn.getBlockY(), "{z}",
                    randomSpawn.getBlockZ()).toLegacyText());
        });
        fillChests(randomSpawn);
    }

    private void fillChests(Location location) {
        CornucopiaLootTable table = new CornucopiaLootTable();
        Random random = new Random();
        for(RelativeBlock b : this.cornucopiaStructure.getBlocks()){
            Block block = b.getLocation(location).getBlock();
            if(block.getType() == Material.CHEST){
                Chest c = (Chest) block.getState();
                System.out.println(String.format("Found chest at %d, %d, %d", block.getX(), block.getY(), block.getZ()));
                table.get(5).forEach(item -> {
                    int slot;
                    do {
                        slot = random.nextInt(c.getBlockInventory().getSize());
                    } while(c.getBlockInventory().getItem(slot) != null);
                    c.getBlockInventory().setItem(slot, item);
                });
            }
        }
    }
}
