package com.mrkirby153.kcuhc.game.spectator;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Objects;

public class SpectatorHandler implements Listener {

    private final UHC uhc;

    public SpectatorHandler(UHC uhc) {
        this.uhc = uhc;

        uhc.getServer().getPluginManager().registerEvents(new SpectatorListener(uhc.getGame()), uhc);
        uhc.getServer().getPluginManager().registerEvents(this, uhc);
    }


    @EventHandler
    public void onJoin(PlayerLoginEvent event) {
        // Add players to the spectator team when the game is alive and they join
        if (uhc.getGame().getCurrentState() == GameState.ALIVE) {
            ScoreboardTeam team = uhc.getGame().getTeam(event.getPlayer());
            if (team == null || !(team instanceof SpectatorTeam)) {
                uhc.getGame().getSpectators().addPlayer(event.getPlayer());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if(event.getType() != UpdateType.FAST)
            return;
        uhc.getGame().getSpectators().getPlayers().stream()
                .map(Bukkit::getPlayer).filter(Objects::nonNull)
                .filter(p -> p.getGameMode() == GameMode.SPECTATOR)
                .forEach(p -> {
            TextComponent component = C.formattedChat("Type ", ChatColor.GREEN);
            component.addExtra(C.formattedChat("/spectate", ChatColor.GOLD, C.Style.BOLD));
            component.addExtra(C.formattedChat(" to return to survival", ChatColor.GREEN));
            uhc.protocolLibManager.sendActionBar(p, component);
        });
        uhc.getGame().getSpectators().getPlayers().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(p -> p.setFireTicks(0));
    }
}
