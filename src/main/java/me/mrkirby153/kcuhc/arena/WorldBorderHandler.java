package me.mrkirby153.kcuhc.arena;

import me.mrkirby153.kcuhc.team.TeamHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldBorderHandler implements Runnable, Listener {


    private double overworldStartSize;
    private double overworldEndSize;
    private double netherStartSize;
    private double netherEndSize;

    private UHCArena arena;

    private BossBar worldborderWarning;

    public WorldBorderHandler(JavaPlugin plugin, UHCArena arena) {
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 20L);
        this.arena = arena;
        this.worldborderWarning = Bukkit.createBossBar(ChatColor.RED + "You are close to the world border!", BarColor.PINK, BarStyle.SOLID);
    }

    public void setWorldborder(double size, int time) {
        WorldBorder overworldBorder = arena.getWorld().getWorldBorder();
        WorldBorder netherBorder = arena.getNether().getWorldBorder();
        overworldStartSize = overworldBorder.getSize();
        overworldEndSize = size;
        overworldBorder.setSize(size, time);
        if (netherBorder != null) {
            netherStartSize = netherBorder.getSize();
            netherEndSize = size * 2;
            netherBorder.setSize(size * 2, time);
        }
    }

    public void setWorldborder(double size) {
        setWorldborder(size, 0);
    }

    public WorldBorder getOverworld() {
        return arena.getWorld().getWorldBorder();
    }

    public WorldBorder getNether() {
        return arena.getNether().getWorldBorder();
    }

    public boolean overworldTravelComplete() {
        return overworldEndSize <= overworldStartSize;
    }

    public boolean netherTravelComplete() {
        return netherEndSize <= netherStartSize;
    }

    public boolean travelComplete() {
        return overworldTravelComplete() && netherTravelComplete();
    }

    public void setWarningDistance(int distance) {
        getOverworld().setWarningDistance(distance);
        getNether().setWarningDistance(distance);
    }

    @Override
    public void run() {
        if(!arena.getProperties().ENABLE_WORLDBORDER_WARNING.get())
            return;
        if (arena.currentState() != UHCArena.State.RUNNING || arena.getWorld().getWorldBorder().getSize() <= arena.getProperties().WORLDBORDER_END_SIZE.get()) {
            worldborderWarning.removeAll();
            return;
        }
        for (Player player : arena.players()) {
            if (TeamHandler.isSpectator(player))
                continue;
            WorldBorder worldborder = player.getWorld().getWorldBorder();
            Location worldBorderCenter = worldborder.getCenter();
            double worldBorderRadius = worldborder.getSize() / 2;
            double worldBorderX = Math.abs(worldBorderCenter.getX() + worldBorderRadius) - 1;
            double worldBorderZ = Math.abs(worldBorderCenter.getZ() + worldBorderRadius) - 1;
            double playerX = Math.abs(player.getLocation().getX());
            double playerZ = Math.abs(player.getLocation().getZ());
            double distX = worldBorderX - playerX;
            double distZ = worldBorderZ - playerZ;

            if (distX < distZ) {
                if (playerX > (worldBorderX - arena.getProperties().WORLDBORDER_WARN_DISTANCE.get())) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, scaleSound(distX), 0.5F);
                    if (!worldborderWarning.getPlayers().contains(player)) {
                        worldborderWarning.addPlayer(player);
                    }
                } else {
                    worldborderWarning.removePlayer(player);
                }
            } else {
                if (playerZ > (worldBorderZ - arena.getProperties().WORLDBORDER_WARN_DISTANCE.get())) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, scaleSound(distZ), 2F);
                    if (!worldborderWarning.getPlayers().contains(player)) {
                        worldborderWarning.addPlayer(player);
                    }
                } else {
                    worldborderWarning.removePlayer(player);
                }
            }

        }
    }

    private float scaleSound(double distance) {
        Integer warnDistance = arena.getProperties().WORLDBORDER_WARN_DISTANCE.get();
        if (distance > warnDistance) {
            distance = warnDistance;
        }
        if (distance < 0)
            distance = 0;
        return (float) (2.0 - (2.0F / warnDistance) * distance);
    }
}
