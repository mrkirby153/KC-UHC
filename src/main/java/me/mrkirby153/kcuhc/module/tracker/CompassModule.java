package me.mrkirby153.kcuhc.module.tracker;

import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class CompassModule extends UHCModule {

    public CompassModule() {
        super(Material.COMPASS, 0, "Give Compass on start", true, "Gives all players a compass when the game starts");
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("Compasses will be given on game start!"));
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("Compasses will no longer be given on game start!"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if(event.getTo() == UHCArena.State.RUNNING){
            for(Player p : getPlugin().arena.players(false)){
                p.getInventory().setItem(8, new ItemFactory(Material.COMPASS).construct());
            }
        }
    }
}
