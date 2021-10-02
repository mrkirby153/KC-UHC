package com.mrkirby153.kcuhc.module.worldborder;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.settings.IntegerSetting;
import com.mrkirby153.kcuhc.module.settings.TimeSetting;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardModuleManager;
import com.mrkirby153.kcuhc.scoreboard.modules.WorldBorderScoreboardModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.protocollib.TitleTimings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.event.EventHandler;

public class WorldBorderModule extends UHCModule {

    private static final int LOBBY_SIZE = 60;
    private static final int DEFAULT_SIZE = 60000;

    private UHC uhc;

    private UHCGame game;

    private TimeSetting time = new TimeSetting("5m");
    private IntegerSetting start = new IntegerSetting(100);
    private IntegerSetting end = new IntegerSetting(50);

    @Inject
    public WorldBorderModule(UHC uhc, UHCGame game) {
        super("World Border", "Controls an automatic worldborder", Material.BARRIER);
        this.game = game;
        autoLoad = true;
        this.uhc = uhc;
    }

    /**
     * Gets the time it will take for the border to move from start to end
     *
     * @return The time, in seconds
     */
    public int getDuration() {
        return (int) (time.getValue() / 1000);
    }

    /**
     * Gets the end size of the border
     *
     * @return The end size of the border
     */
    public int getEndSize() {
        return end.getValue();
    }

    /**
     * Gets the start size of the worldborder
     *
     * @return The start size of the border
     */
    public int getStartSize() {
        return start.getValue();
    }

    @Override
    public void onLoad() {
        this.game.getWorldBorder().setSize(LOBBY_SIZE);
        this.game.getWorldBorder().setWarningDistance(0);
        ScoreboardModuleManager.INSTANCE.installModule(new WorldBorderScoreboardModule(game, this), -1);
    }

    @Override
    public void onUnload() {
        this.game.getWorldBorder().setSize(DEFAULT_SIZE);
        ScoreboardModuleManager.INSTANCE.removeModule(WorldBorderScoreboardModule.class);
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ENDING || event.getTo() == GameState.WAITING) {
            this.game.getWorldBorder().setSize(LOBBY_SIZE);
            this.game.getWorldBorder().setWarningDistance(0);
        }
        if (event.getTo() == GameState.ALIVE) {
            this.game.getWorldBorder().setSize(getStartSize());
            this.game.getWorldBorder().setSize(getEndSize(), getDuration());
            System.out.println(
                "Moving " + (getStartSize() - getEndSize()) + " blocks in " + Time.INSTANCE
                    .format(0, getDuration() * 1000));
            this.game.getWorldBorder().setWarningDistance(50);
        }
    }

    /**
     * Resolves a stalemate
     */
    public void resolveStalemate() {
        this.game.getWorldBorder().setSize(1, 60 * 10);
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(Chat.message("Stalemate",
                "Stalemate detected! Worldborder shrinking to one block over 10 minutes")
                .toLegacyText());
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1F, 1F);
            uhc.protocolLibManager
                .title(p, ChatColor.GOLD + "Stalemate Detected", "Shrinking world border",
                    new TitleTimings(10, 60, 10));
        });
    }

    public void updateSpeed(int newDuration) {
        WorldBorder border = this.game.getWorldBorder();
        double size = border.getSize();
        border.setSize(size); // Reset the border to its current position
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.spigot().sendMessage(Chat.message("World Border",
                "Alert! Moving from {startSize} to {endSize} in {duration}",
                "{startSize}", Time.INSTANCE.trim(2, size),
                "{endSize}", getEndSize(),
                "{duration}", Time.INSTANCE.format(1, newDuration * 1000, Time.TimeUnit.FIT)));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 1F);
        });
        border.setSize(getEndSize(), newDuration);
    }

    public double[] worldborderLoc() {
        WorldBorder wb = this.game.getWorldBorder();
        Location l = wb.getCenter();
        double locX = (wb.getSize() / 2) + l.getX();
        double locY = (wb.getSize() / 2) + l.getY();
        return new double[]{locX, locY};
    }
}
