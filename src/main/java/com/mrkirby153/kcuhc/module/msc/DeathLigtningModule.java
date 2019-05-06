package com.mrkirby153.kcuhc.module.msc;

import com.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathLigtningModule extends UHCModule {

    public DeathLigtningModule() {
        super("Death Lightning", "Spawns a fake lightning bolt on death", Material.GOLDEN_SWORD);
        autoLoad = true;
    }

    @EventHandler
    public void playerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        dead.getWorld().strikeLightningEffect(dead.getLocation());
    }
}
