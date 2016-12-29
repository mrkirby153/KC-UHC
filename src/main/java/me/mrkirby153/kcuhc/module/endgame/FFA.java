package me.mrkirby153.kcuhc.module.endgame;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.utils.UtilChat;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class FFA extends EndgameScenario {

    public FFA() {
        super(Material.DIAMOND_SWORD, 0, "Free for All", "Last person standing wins!");
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("Last person standing wins"));
    }


    @Override
    public void update() {
        int playerCount = 0;
        for (Player p : UHC.getInstance().arena.players()) {
            if (UHC.getInstance().teamHandler.isSpectator(p))
                continue;
            playerCount++;
        }
        if (playerCount <= 1) {
            if (playerCount == 1) {
                stop(UHC.getInstance().arena.players().get(0).getDisplayName(), Color.WHITE);
            } else {
                stop("Nobody", Color.WHITE);
            }
        }
    }
}