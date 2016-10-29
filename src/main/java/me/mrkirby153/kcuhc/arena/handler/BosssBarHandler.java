package me.mrkirby153.kcuhc.arena.handler;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.UUID;

public class BosssBarHandler implements Listener {

    private static HashMap<UUID, BossBar> playerBossBars = new HashMap<>();

    public static BossBar getBar(Player player) {
        return playerBossBars.get(player.getUniqueId());
    }

    public static void removeAll() {
        Bukkit.getOnlinePlayers().forEach(BosssBarHandler::removeBar);
    }

    public static void removeBar(Player player) {
        BossBar bar = playerBossBars.remove(player.getUniqueId());
        if (bar == null)
            return;
        bar.removeAll();
    }

    public static void setBossBarColor(Player player, BarColor color) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar == null)
            return;
        bar.setColor(color);
    }

    public static void setBossBarProgress(Player player, double percent) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar == null)
            return;
        bar.setProgress(percent);
    }

    public static void setBossBarStyle(Player player, BarStyle style) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar == null)
            return;
        bar.setStyle(style);
    }

    public static void setBossBarText(Player player, String text) {
        BossBar bar = playerBossBars.get(player.getUniqueId());
        if ((text == null || text.isEmpty()) && bar != null) {
            bar.removeAll();
            playerBossBars.remove(player.getUniqueId());
            return;
        }
        if (bar == null) {
            playerBossBars.put(player.getUniqueId(), bar = Bukkit.createBossBar(text, BarColor.PINK, BarStyle.SOLID));
            bar.addPlayer(player);
        }
        bar.setTitle(text);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerBossBars.remove(event.getPlayer().getUniqueId());
    }
}
