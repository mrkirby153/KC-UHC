package me.mrkirby153.kcuhc.module.worldborder;

import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.event.EventHandler;

import java.util.Optional;

public class WorldBorderModule extends UHCModule {

    private double startSize;
    private double endSize;

    public WorldBorderModule() {
        super(Material.BEDROCK, 0, "World Border", true, "Gives control of the world border to this plugin");
    }

    public double getEndSize() {
        return endSize;
    }

    public Optional<WorldBorder> getNetherWorldborder() {
        if (getPlugin().arena.getNether() == null)
            return Optional.empty();
        return Optional.of(getPlugin().arena.getNether().getWorldBorder());
    }

    public WorldBorder getOverworldBorder() {
        return getPlugin().arena.getWorld().getWorldBorder();
    }

    public double getStartSize() {
        return startSize;
    }

    public boolean travelComplete() {
        return getOverworldBorder().getSize() <= endSize;
    }

    public double[] worldborderLoc() {
        WorldBorder wb = getOverworldBorder();
        Location l = wb.getCenter();
        double locX = (wb.getSize() / 2) + l.getX();
        double locZ = (wb.getSize() / 2) + l.getZ();
        return new double[]{locX, locZ};
    }

    @Override
    public void onEnable() {

    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        ArenaProperties properties = getPlugin().arena.getProperties();
        if (event.getTo() == UHCArena.State.INITIALIZED) {
            setWorldBorder(60);
            setWarningDistance(0);
        }
        if (event.getTo() == UHCArena.State.RUNNING) {
            setWorldBorder(properties.WORLDBORDER_START_SIZE.get());
            setWarningDistance(properties.WORLDBORDER_WARN_DISTANCE.get());
            setWorldBorder(properties.WORLDBORDER_END_SIZE.get(), properties.WORLDBORDER_TRAVEL_TIME.get() * 60);
        }
        if (event.getTo() == UHCArena.State.ENDGAME) {
            setWorldBorder(60);
            setWarningDistance(0);
        }
    }

    public void setWarningDistance(int distance) {
        getOverworldBorder().setWarningDistance(distance);
        getNetherWorldborder().ifPresent(wb -> wb.setWarningDistance(distance));
    }

    public void setWorldBorder(double size) {
        this.setWorldBorder(size, 0);
    }

    public void setWorldBorder(double size, int time) {
        this.startSize = getOverworldBorder().getSize();
        getOverworldBorder().setSize(this.endSize = size, time);
        getNetherWorldborder().ifPresent(wb -> wb.setSize(size / 8D, time));
    }

}
