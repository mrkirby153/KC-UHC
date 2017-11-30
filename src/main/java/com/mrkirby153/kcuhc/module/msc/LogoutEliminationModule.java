package com.mrkirby153.kcuhc.module.msc;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class LogoutEliminationModule extends UHCModule {

    private static final int LOGOUT_TIME_MINUTES = 5;

    private HashMap<UUID, LoggedOutPlayer> loggedOutPlayers = new HashMap<>();

    private UHC uhc;

    @Inject
    public LogoutEliminationModule(UHC uhc) {
        super("Eliminate on Logout", "Eliminates the player 5 minutes after they log out",
            Material.TNT);
        this.uhc = uhc;
        this.autoLoad = true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (loggedOutPlayers.containsKey(event.getPlayer().getUniqueId())) {
            loggedOutPlayers.remove(event.getPlayer().getUniqueId());
            Bukkit.getOnlinePlayers().forEach(p -> {
                p.sendMessage(Chat.INSTANCE.message("Login", "{player} has rejoined!",
                    "{player}", event.getPlayer().getName()).toLegacyText());
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F);
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        ScoreboardTeam team = this.uhc.getGame().getTeam(event.getPlayer());
        if (team == null || team instanceof SpectatorTeam) {
            return;
        }
        long eliminationTime = System.currentTimeMillis() + (1000 * LOGOUT_TIME_MINUTES * 60);
        LoggedOutPlayer player = new LoggedOutPlayer(event.getPlayer().getName(),
            team.getTeamName(),
            eliminationTime);
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(
                Chat.INSTANCE.message("Logout", "{player} has logged out, {time} to rejoin",
                    "{player}", event.getPlayer().getName(),
                    "{time}", Time.INSTANCE
                        .format(1, eliminationTime - System.currentTimeMillis(), Time.TimeUnit.FIT))
                    .toLegacyText());
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F);
        });
        this.loggedOutPlayers.put(event.getPlayer().getUniqueId(), player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        List<UUID> toRemove = new ArrayList<>();
        loggedOutPlayers.forEach((uuid, player) -> {
            if (player.eliminationTime <= System.currentTimeMillis()) {
                // Remove the player from the team
                UHCTeam team = this.uhc.getGame().getTeam(player.teamName);
                if (team != null) {
                    team.removePlayer(uuid);
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        p.sendMessage(Chat.INSTANCE.message("Game",
                            "{player} has been offline for too long and has been eliminated!",
                            "{player}", player.name).toLegacyText());
                        p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 1F, 1F);
                    });
                }
                toRemove.add(uuid);
            }
        });
        toRemove.forEach(loggedOutPlayers::remove);
    }

    private class LoggedOutPlayer {

        private final String name;
        private final String teamName;
        private final long eliminationTime;

        private LoggedOutPlayer(String name, String teamName, long eliminationTime) {
            this.teamName = teamName;
            this.eliminationTime = eliminationTime;
            this.name = name;
        }
    }

}
