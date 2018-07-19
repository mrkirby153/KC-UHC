package com.mrkirby153.kcuhc.module.msc;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.settings.IntegerSetting;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.Chat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

public class HeightBuildingModule extends UHCModule {

    private static final int BUILD_RADIUS = 50;

    private IntegerSetting height = new IntegerSetting(100);

    private UHCGame game;

    @Inject
    public HeightBuildingModule(UHCGame game) {
        super("Height Restriction", "Prevents building above a certain height near spawn",
            Material.IRON_DOOR);
        this.game = game;
        autoLoad = true;
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
            if (builtBlock.getBlockY() >= height.getValue()) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(
                    Chat.error("You cannot build this high near spawn").toLegacyText());
            }
        }
    }
}
