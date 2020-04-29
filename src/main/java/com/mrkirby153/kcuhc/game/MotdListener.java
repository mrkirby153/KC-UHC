package com.mrkirby153.kcuhc.game;

import com.mrkirby153.kcuhc.Strings;
import com.mrkirby153.kcuhc.UHC;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public class MotdListener implements Listener {

    private UHC plugin;

    public MotdListener(UHC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onServerListPing(ServerListPingEvent event) {
        String motd =
            ChatColor.GOLD + "" + ChatColor.BOLD + Strings.LONG_NAME + ":\n" + ChatColor.GRAY;
        switch (plugin.getGame().getCurrentState()) {
            case WAITING:
                motd += "Waiting for start";
                break;
            case COUNTDOWN:
                motd += "Game starting";
                break;
            case ALIVE:
                motd += "Game running. Join to spectate";
                break;
            case ENDING:
                motd += "Game ending";
                break;
            case ENDED:
                motd += "Game has ended";
                break;
        }

        event.setMotd(motd);
    }
}
