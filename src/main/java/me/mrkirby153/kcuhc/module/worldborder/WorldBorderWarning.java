package me.mrkirby153.kcuhc.module.worldborder;

import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class WorldBorderWarning extends UHCModule {

    public WorldBorderWarning() {
        super(Material.NOTE_BLOCK, 0, "World Border Warning", true, "Notifies users as they get closer to the world border");
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("World Border Warning disabled!"));
        Bukkit.getOnlinePlayers().forEach(getPlugin().bossBar::remove);
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("World Border Warning enabled!"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (getPlugin().arena.currentState() != UHCArena.State.RUNNING || getPlugin().arena.getWorld().getWorldBorder().getSize() <= getPlugin().arena.getProperties().WORLDBORDER_END_SIZE.get()) {
            if (getPlugin().arena.currentState() == UHCArena.State.COUNTDOWN)
                return;
            for (Player p : getPlugin().arena.players()) {
                getPlugin().bossBar.remove(p);
            }
            return;
        }
        for (Player player : getPlugin().arena.players()) {
            if (getPlugin().teamHandler.isSpectator(player))
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
                if (playerX > (worldBorderX - getPlugin().arena.getProperties().WORLDBORDER_WARN_DISTANCE.get())) {
                    if (event.getType() == UpdateType.SECOND)
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, scaleSound(distX), 0.5F);
                    getPlugin().bossBar.setTitle(player, ChatColor.RED + "You are close to the world border!");
                    double percent = 1 - (worldBorderX - playerX) / getPlugin().arena.getProperties().WORLDBORDER_WARN_DISTANCE.get();
                    if (event.getType() == UpdateType.TICK)
                        getPlugin().bossBar.setPercent(player, percent);
                } else {
                    if (event.getType() == UpdateType.TICK)
                        getPlugin().bossBar.remove(player);
                }
            } else {
                if (playerZ > (worldBorderZ - getPlugin().arena.getProperties().WORLDBORDER_WARN_DISTANCE.get())) {
                    if (event.getType() == UpdateType.SECOND)
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, scaleSound(distZ), 2F);
                    getPlugin().bossBar.setTitle(player, ChatColor.RED + "You are close to the world border!");
                    double percent = 1 - (worldBorderZ - playerZ) / getPlugin().arena.getProperties().WORLDBORDER_WARN_DISTANCE.get();
                    if (event.getType() == UpdateType.TICK)
                        getPlugin().bossBar.setPercent(player, percent);
                } else {
                    if (event.getType() == UpdateType.TICK)
                        getPlugin().bossBar.remove(player);
                }
            }

        }
    }

    private float scaleSound(double distance) {
        Integer warnDistance = getPlugin().arena.getProperties().WORLDBORDER_WARN_DISTANCE.get();
        if (distance > warnDistance) {
            distance = warnDistance;
        }
        if (distance < 0)
            distance = 0;
        return (float) (2.0 - (2.0F / warnDistance) * distance);
    }
}
