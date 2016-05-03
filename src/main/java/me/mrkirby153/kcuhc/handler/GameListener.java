package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {

    @EventHandler
    public void death(PlayerDeathEvent event) {
        event.getEntity().setGlowing(false);
        UHC.arena.handleDeathMessage(event.getEntity(), event.getDeathMessage());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UHC.arena.playerJoin(event.getPlayer());
        if (TeamHandler.getTeamForPlayer(event.getPlayer()) == null)
            TeamHandler.joinTeam(TeamHandler.spectatorsTeam(), event.getPlayer());
        else
            TeamHandler.joinTeam(TeamHandler.getTeamForPlayer(event.getPlayer()), event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage(ChatColor.RED + event.getPlayer().getName() + " has left!");
        UHC.arena.playerDisconnect(event.getPlayer());
    }
}
