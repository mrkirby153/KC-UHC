package com.mrkirby153.kcuhc.module.worldborder;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.floodgate.FloodgateWrapper;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.settings.EnumSetting;
import com.mrkirby153.kcuhc.module.settings.IntegerSetting;
import com.mrkirby153.kcuhc.module.settings.ModuleSetting;
import com.mrkirby153.kcuhc.module.settings.TimeSetting;
import com.mrkirby153.kcuhc.player.ActionBar;
import com.mrkirby153.kcuhc.player.ActionBarManager;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardModuleManager;
import com.mrkirby153.kcuhc.scoreboard.modules.WorldBorderScoreboardModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.protocollib.TitleTimings;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class WorldBorderModule extends UHCModule {

    private static final int LOBBY_SIZE = 60;
    private static final int DEFAULT_SIZE = 60000;

    private UHC uhc;

    private UHCGame game;

    private TimeSetting time = new TimeSetting("5m");
    private IntegerSetting start = new IntegerSetting(100);
    private IntegerSetting end = new IntegerSetting(50);
    private IntegerSetting warningDistance = new IntegerSetting(50);
    private EnumSetting<AltWarningMode> altWarningMode = new EnumSetting<>(
        AltWarningMode.BEDROCK_ONLY, AltWarningMode.class);

    private ActionBar worldBorderActionBar;

    @Inject
    public WorldBorderModule(UHC uhc, UHCGame game) {
        super("World Border", "Controls an automatic worldborder", Material.BARRIER);
        this.game = game;
        autoLoad = true;
        this.uhc = uhc;
        this.worldBorderActionBar = new ActionBar("worldborder", Integer.MAX_VALUE);
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
        ScoreboardModuleManager.INSTANCE.installModule(new WorldBorderScoreboardModule(game, this),
            -1);
        ActionBarManager.getInstance().registerActionBar(worldBorderActionBar);
    }

    @Override
    public void onUnload() {
        this.game.getWorldBorder().setSize(DEFAULT_SIZE);
        ScoreboardModuleManager.INSTANCE.removeModule(WorldBorderScoreboardModule.class);
        worldBorderActionBar.clearAll();
        ActionBarManager.getInstance().unregisterActionBar(worldBorderActionBar);
    }

    @Override
    public void onSettingChange(ModuleSetting<?> setting) {
        if (setting == altWarningMode) {
            worldBorderActionBar.clearAll();
        }
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
                "Moving " + (getStartSize() - getEndSize()) + " blocks in " + Time
                    .format(0, getDuration() * 1000L));
            this.game.getWorldBorder().setWarningDistance(warningDistance.getValue());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.FAST
            && altWarningMode.getValue() != AltWarningMode.NONE) {
            Bukkit.getOnlinePlayers().forEach(pl -> {
                switch (altWarningMode.getValue()) {
                    case ALL:
                        updateWarningActionbar(pl);
                        break;
                    case BEDROCK_ONLY:
                        if (FloodgateWrapper.getFloodgate().isFloodgatePlayer(pl.getUniqueId())) {
                            updateWarningActionbar(pl);
                        } else {
                            worldBorderActionBar.clear(pl);
                        }
                        break;
                }
            });
        }
    }

    private void updateWarningActionbar(Player player) {
        Location playerPos = player.getLocation();
        World world = playerPos.getWorld();
        if (world != null && world.getWorldBorder().getWarningDistance() > 0) {
            Location wbCenter = world.getWorldBorder().getCenter();

            double locX = (world.getWorldBorder().getSize() / 2.0) + wbCenter.getX();
            double locZ = (world.getWorldBorder().getSize() / 2.0) + wbCenter.getX();

            Location southWorldBorderLocation = playerPos.clone();
            Location northWorldBorderLocation = playerPos.clone();
            Location eastWorldBorderLocation = playerPos.clone();
            Location westWorldBorderLocation = playerPos.clone();

            southWorldBorderLocation.setZ(Math.abs(locZ));
            northWorldBorderLocation.setZ(Math.abs(locZ) * -1);

            eastWorldBorderLocation.setX(Math.abs(locX));
            westWorldBorderLocation.setX(Math.abs(locX) * -1);

            double distanceSouth = southWorldBorderLocation.distanceSquared(playerPos);
            double distanceNorth = northWorldBorderLocation.distanceSquared(playerPos);
            double distanceEast = eastWorldBorderLocation.distanceSquared(playerPos);
            double distanceWest = westWorldBorderLocation.distanceSquared(playerPos);

            double distanceSquared = distanceWest;
            if (distanceSouth < distanceNorth && distanceSouth < distanceEast
                && distanceSouth < distanceWest) {
                distanceSquared = distanceSouth;
            } else if (distanceNorth < distanceEast && distanceNorth < distanceWest) {
                distanceSquared = distanceNorth;
            } else if (distanceEast < distanceWest) {
                distanceSquared = distanceEast;
            }

            boolean outsideWorldborder = false;
            if(Math.abs(playerPos.getX()) > locX || Math.abs(playerPos.getZ()) > locZ) {
                outsideWorldborder = true;
            }

            double distance = Math.sqrt(distanceSquared);
            if(distance < world.getWorldBorder().getWarningDistance() || outsideWorldborder) {
                double distancePercent = 1 - (distance / world.getWorldBorder().getWarningDistance());
                if(outsideWorldborder) {
                    distancePercent = 1;
                }
                int totalSquares = 20;
                int filledSquares = (int) Math.ceil(distancePercent * totalSquares);

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < totalSquares; i++) {
                    sb.append(i < filledSquares ? ChatColor.DARK_RED : ChatColor.GREEN).append("|");
                }
                sb.append(ChatColor.WHITE);

                BaseComponent component = Chat.formattedChat("World Border: ",
                    net.md_5.bungee.api.ChatColor.WHITE);
                component.addExtra("[" + sb + "]");
                worldBorderActionBar.set(player, component);
            } else {
                worldBorderActionBar.clear(player);
            }
        } else {
            worldBorderActionBar.clear(player);
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

    public enum AltWarningMode {
        NONE,
        BEDROCK_ONLY,
        ALL
    }

    private enum ClosestWorldborder {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }
}
