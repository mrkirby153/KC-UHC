package com.mrkirby153.kcuhc.module.msc;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Objects;

public class TeamChatModule extends UHCModule {

    private UHC uhc;

    @Inject
    public TeamChatModule(UHC uhc) {
        super("Team Chat", "Restrict chat on the server to teams", Material.OAK_SIGN);
        this.uhc = uhc;
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (uhc.getGame().getCurrentState() != GameState.ALIVE) {
            return;
        }
        ScoreboardTeam team = uhc.getGame().getTeam(event.getPlayer());

        if (team == null) {
            return;
        }
        if (event.getMessage().startsWith("@") && !(team instanceof SpectatorTeam)) {
            event.setMessage(event.getMessage().substring(1));
            return;
        }
        if (team instanceof SpectatorTeam) {
            event.setFormat(ChatColor.GRAY + "[SPECTATOR] " + ChatColor.RESET + event.getFormat());
        } else {
            event.setFormat(
                ChatColor.AQUA + "" + ChatColor.BOLD + "TEAM> " + ChatColor.RESET + event.getFormat());
        }
        event.setCancelled(true);
        team.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
            .forEach(p -> p.sendMessage(
                String.format(event.getFormat(), event.getPlayer().getName(), event.getMessage())));
    }
}
