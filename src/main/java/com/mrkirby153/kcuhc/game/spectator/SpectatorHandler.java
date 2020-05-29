package com.mrkirby153.kcuhc.game.spectator;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.player.ActionBar;
import com.mrkirby153.kcuhc.player.ActionBarManager;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Chat.Style;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpectatorHandler implements Listener {

    private final UHC uhc;
    private final ActionBar spectatorActionBar = new ActionBar("spectator", 5);
    public HashSet<UUID> pendingSpectators = new HashSet<>();

    @Inject
    public SpectatorHandler(UHC uhc) {
        this.uhc = uhc;

        uhc.getServer().getPluginManager()
            .registerEvents(new SpectatorListener(uhc.getGame()), uhc);
        uhc.getServer().getPluginManager().registerEvents(this, uhc);

        ActionBarManager.getInstance().registerActionBar(spectatorActionBar);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.COUNTDOWN || event.getTo() == GameState.ALIVE) {
            pendingSpectators.stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
                .forEach(e -> {
                    uhc.getGame().getSpectators().addPlayer(e);
                });
        }
        if (event.getTo() == GameState.ENDING || event.getTo() == GameState.ENDED) {
            List<Player> players = new ArrayList<>(
                uhc.getGame().getSpectators().getPlayers().stream()
                    .map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList()));
            players.forEach(p -> uhc.getGame().getSpectators().removePlayer(p));
            pendingSpectators.clear();
        }
    }

    @EventHandler
    public void onJoin(PlayerLoginEvent event) {
        // Add players to the spectator team when the game is alive and they join
        // Hide all spectators from the player

        // TODO: 7/30/2017 Fix rejoining being placed on spectator team
        if(uhc.getGame().getCurrentState() == GameState.ALIVE) {
            ScoreboardTeam team = uhc.getGame().getTeam(event.getPlayer());
            if(team == null || team instanceof SpectatorTeam) {
                if(!event.getPlayer().hasPermission("kcuhc.spectate")) {
                    event.setResult(Result.KICK_OTHER);
                    event.setKickMessage("You do not have permission to spectate");
                    return;
                }
            }
        }
        Bukkit.getServer().getScheduler().runTask(uhc, () -> {
            uhc.getGame().getSpectators().getPlayers().stream().map(Bukkit::getPlayer)
                .filter(Objects::nonNull).forEach(e -> {
                event.getPlayer().hidePlayer(uhc, e);
            });
            if (uhc.getGame().getCurrentState() == GameState.ALIVE) {
                ScoreboardTeam team = uhc.getGame().getTeam(event.getPlayer());
                if (team == null) {
                    uhc.getGame().getSpectators().addPlayer(event.getPlayer());
                } else {
                    if (team instanceof SpectatorTeam) {
                        team.removePlayer(event.getPlayer());
                        team.addPlayer(event.getPlayer());
                    }
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        uhc.getGame().getSpectators().removePlayer(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.FAST) {
            return;
        }

        uhc.getGame().getSpectators().getPlayers().stream().map(Bukkit::getPlayer)
            .filter(Objects::nonNull).forEach(player -> {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                TextComponent component = Chat.formattedChat("Type ", ChatColor.GREEN);
                component.addExtra(Chat.formattedChat("/spectate", ChatColor.GOLD, Style.BOLD));
                component.addExtra(Chat.formattedChat(" to return to survival", ChatColor.GREEN));
                spectatorActionBar.set(player, component);
            } else {
                if (spectatorActionBar.get(player) != null) {
                    spectatorActionBar.clear(player);
                }
            }
        });
        uhc.getGame().getSpectators().getPlayers().stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .forEach(p -> p.setFireTicks(0));
    }
}
