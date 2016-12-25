package me.mrkirby153.kcuhc.module.health;

import me.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldLoadEvent;

public class NaturalRegenerationModule extends UHCModule {

    protected static final String GAME_RULE = "naturalRegeneration";

    public NaturalRegenerationModule() {
        super(Material.POTION, 0, "Disable Natural Regeneration", true, "Disables natural regeneration");
        this.unregisterListener = false;
    }

    @Override
    public void onDisable() {
        for (World w : Bukkit.getWorlds()) {
            w.setGameRuleValue(GAME_RULE, "false");
        }
    }

    @Override
    public void onEnable() {
        for (World w : Bukkit.getWorlds()) {
            w.setGameRuleValue(GAME_RULE, "true");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent event) {
        event.getWorld().setGameRuleValue(GAME_RULE, isLoaded() ? "false" : "true");
    }
}
