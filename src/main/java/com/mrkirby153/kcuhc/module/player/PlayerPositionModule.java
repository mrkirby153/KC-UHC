package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;


public class PlayerPositionModule extends UHCModule {

    private UHC uhc;
    private UHCGame game;

    @Inject
    public PlayerPositionModule(UHC uhc, UHCGame game) {
        super("Player position", "Show the player's position above the hotbar", Material.MAP);
        this.uhc = uhc;
        this.game = game;
        this.autoLoad = true;
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.FAST)
            return;
        if (game.getCurrentState() != GameState.ALIVE)
            return;
        Bukkit.getOnlinePlayers().stream()
                .filter(Player::isValid)
                .filter(p -> !(uhc.getGame().getTeam(p) instanceof SpectatorTeam))
                .forEach(p -> {
                    Location l = p.getLocation();
                    TextComponent bc;
                    bc = C.formattedChat("Current Position: ", ChatColor.GOLD);
                    bc.addExtra(C.formattedChat("X: ", ChatColor.RED));
                    bc.addExtra(C.formattedChat(String.format("%.2f", l.getX()), ChatColor.GREEN));
                    bc.addExtra(C.formattedChat(" Y: ", ChatColor.RED));
                    bc.addExtra(C.formattedChat(String.format("%.2f", l.getY()), ChatColor.GREEN));
                    bc.addExtra(C.formattedChat(" Z: ", ChatColor.RED));
                    bc.addExtra(C.formattedChat(String.format("%.2f", l.getZ()), ChatColor.GREEN));
                    this.uhc.protocolLibManager.sendActionBar(p, bc);
                });
    }
}
