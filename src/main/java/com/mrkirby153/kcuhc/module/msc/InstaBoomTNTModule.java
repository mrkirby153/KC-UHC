package com.mrkirby153.kcuhc.module.msc;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

public class InstaBoomTNTModule extends UHCModule {

    private final UHCGame game;

    @Inject
    public InstaBoomTNTModule(UHCGame game) {
        super("Insta-Boom TNT", "TNT lights immediately on place", Material.TNT);
        this.game = game;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.TNT) {
            // Light the TNT
            event.getBlockPlaced().setType(Material.AIR);
            Location l = event.getBlockPlaced().getLocation();
            event.getBlockPlaced().getWorld().spawnEntity(l, EntityType.PRIMED_TNT);
        }
    }
}
