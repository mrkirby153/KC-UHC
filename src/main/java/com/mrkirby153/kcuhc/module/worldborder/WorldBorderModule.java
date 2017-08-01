package com.mrkirby153.kcuhc.module.worldborder;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.protocollib.TitleTimings;
import org.bukkit.*;
import org.bukkit.event.EventHandler;

public class WorldBorderModule extends UHCModule {

    private static final int LOBBY_SIZE = 60;
    private static final int DEFAULT_SIZE = 60000;

    private int startSize = 100;
    private int endSize = 50;

    private int duration = 300; // Default to 5 minutes

    private UHC uhc;

    @Inject
    public WorldBorderModule(UHC uhc) {
        super("World Border", "Controls an automatic worldborder", Material.BARRIER);
        autoLoad = true;
        this.uhc = uhc;
    }

    /**
     * Gets the time it will take for the border to move from start to end
     *
     * @return The time, in seconds
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Resolves a stalemate
     */
    public void resolveStalemate() {
        UHC.getUHCWorld().getWorldBorder().setSize(1, 60 /* * 10*/);
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(C.m("Stalemate", "Stalemate detected! Worldborder shrinking to one block over 10 minutes").toLegacyText());
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1F, 1F);
            uhc.protocolLibManager.title(p, ChatColor.GOLD+"Stalemate Detected", "Shrinking world border", new TitleTimings(10, 60, 10));
        });
    }

    /**
     * Sets the duration of the border
     *
     * @param duration The duration
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Gets the end size of the border
     *
     * @return The end size of the border
     */
    public int getEndSize() {
        return endSize;
    }

    /**
     * Sets the end size of the border
     *
     * @param endSize The end size of the border
     */
    public void setEndSize(int endSize) {
        this.endSize = endSize;
    }

    /**
     * Gets the start size of the worldborder
     *
     * @return The start size of the border
     */
    public int getStartSize() {
        return startSize;
    }

    /**
     * Sets the start size of the border
     *
     * @param startSize The size of the border
     */
    public void setStartSize(int startSize) {
        this.startSize = startSize;
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ENDING || event.getTo() == GameState.WAITING) {
            UHC.getUHCWorld().getWorldBorder().setSize(LOBBY_SIZE);
            UHC.getUHCWorld().getWorldBorder().setWarningDistance(0);
        }
        if (event.getTo() == GameState.ALIVE) {
            UHC.getUHCWorld().getWorldBorder().setSize(startSize);
            UHC.getUHCWorld().getWorldBorder().setSize(endSize, duration);
            System.out.println("Moving " + (startSize - endSize) + " blocks in " + duration + " seconds");
            UHC.getUHCWorld().getWorldBorder().setWarningDistance(50);
        }
    }

    @Override
    public void onLoad() {
        UHC.getUHCWorld().getWorldBorder().setSize(LOBBY_SIZE);
        UHC.getUHCWorld().getWorldBorder().setWarningDistance(0);
    }

    @Override
    public void onUnload() {
        UHC.getUHCWorld().getWorldBorder().setSize(DEFAULT_SIZE);
    }

    public void updateSpeed(int newDuration) {
        WorldBorder border = UHC.getUHCWorld().getWorldBorder();
        double size = border.getSize();
        border.setSize(size); // Reset the border to its current position
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.spigot().sendMessage(C.m("World Border", "Alert! Moving from {startSize} to {endSize} in {duration}",
                    "{startSize}", size,
                    "{endSize}", endSize,
                    "{duration}", Time.format(1, newDuration * 1000, Time.TimeUnit.FIT)));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F);
        });
        border.setSize(endSize, newDuration);
    }

    public double[] worldborderLoc() {
        WorldBorder wb = UHC.getUHCWorld().getWorldBorder();
        Location l = wb.getCenter();
        double locX = (wb.getSize() / 2) + l.getX();
        double locY = (wb.getSize() / 2) + l.getY();
        return new double[]{locX, locY};
    }
}
