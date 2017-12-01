package com.mrkirby153.kcuhc.module.msc;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.Chat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Optional;

public class HeightBuildingModule extends UHCModule {

    private static final int BUILD_RADIUS = 50;
    private int MAX_BUILD_HEIGHT = 100;

    private UHCGame game;

    @Inject
    public HeightBuildingModule(UHCGame game) {
        super("Height Restriction", "Prevents building above a certain height near spawn",
            Material.IRON_DOOR);
        this.game = game;
        autoLoad = true;
    }

    public int getMaxBuildHeight() {
        return this.MAX_BUILD_HEIGHT;
    }

    public void setMaxBuildHeight(int height) {
        this.MAX_BUILD_HEIGHT = height;
    }

    @Override
    public void loadData(HashMap<String, String> data) {
        MAX_BUILD_HEIGHT = Integer.valueOf(data.get("max-build-height"));
    }

    @Override
    public void saveData(HashMap<String, String> data) {
        data.put("max-build-height", Integer.toString(MAX_BUILD_HEIGHT));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        int buildRadius = BUILD_RADIUS;

        Location center;
        Optional<WorldBorderModule> mod = ModuleRegistry.INSTANCE
            .getLoadedModule(WorldBorderModule.class);
        if (mod.isPresent()) {
            center = this.game.getUHCWorld().getWorldBorder().getCenter().clone();
            buildRadius = mod.get().getEndSize() / 2;
        } else {
            center = this.game.getUHCWorld().getSpawnLocation();
        }

        Location builtBlock = event.getBlockPlaced().getLocation().clone();
        center.setY(builtBlock.getBlockY());

        if (center.distanceSquared(builtBlock) <= Math.pow(buildRadius, 2)) {
            if (builtBlock.getBlockY() >= MAX_BUILD_HEIGHT) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(
                    Chat.INSTANCE.error("You cannot build this high near spawn").toLegacyText());
            }
        }
    }
}
