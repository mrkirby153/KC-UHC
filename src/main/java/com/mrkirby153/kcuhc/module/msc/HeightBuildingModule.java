package com.mrkirby153.kcuhc.module.msc;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.C;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

public class HeightBuildingModule extends UHCModule {

    private int MAX_BUILD_HEIGHT = 100;

    private static final int BUILD_RADIUS = 50;

    public HeightBuildingModule(){
        super("Height Restriction", "Prevents building above a certain height near spawn", Material.IRON_DOOR);
        autoLoad = true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        int buildRadius = BUILD_RADIUS;

        Location center;
        Optional<WorldBorderModule> mod = ModuleRegistry.INSTANCE.getLoadedModule(WorldBorderModule.class);
        if(mod.isPresent()){
            center = UHC.getUHCWorld().getWorldBorder().getCenter().clone();
            buildRadius = mod.get().getEndSize() / 2;
        } else {
            center = UHC.getUHCWorld().getSpawnLocation();
        }

        Location builtBlock = event.getBlockPlaced().getLocation().clone();
        center.setY(builtBlock.getBlockY());

        if(center.distanceSquared(builtBlock) <= Math.pow(buildRadius, 2)){
            if(builtBlock.getBlockY() >= MAX_BUILD_HEIGHT) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(C.e("You cannot build this high near spawn").toLegacyText());
            }
        }
    }

    public void setMaxBuildHeight(int height){
        this.MAX_BUILD_HEIGHT = height;
    }

    public int getMaxBuildHeight(){
        return this.MAX_BUILD_HEIGHT;
    }
}
