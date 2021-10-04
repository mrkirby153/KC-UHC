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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorldBorderModule extends UHCModule {

    private static final int LOBBY_SIZE = 60;
    private static final int DEFAULT_SIZE = 60000;

    private UHC uhc;

    private UHCGame game;

    private TimeSetting time = new TimeSetting("5m");
    private IntegerSetting start = new IntegerSetting(100);
    private IntegerSetting end = new IntegerSetting(50);
    private IntegerSetting warningDistance = new IntegerSetting(50);
    private EnumSetting<PlatformTarget> altWarningMode = new EnumSetting<>(
        PlatformTarget.BEDROCK_ONLY, PlatformTarget.class);
    private EnumSetting<Material> borderWall = new EnumSetting<>(Material.BEDROCK, Material.class);
    private EnumSetting<PlatformTarget> borderWallEnabled = new EnumSetting<>(
        PlatformTarget.BEDROCK_ONLY, PlatformTarget.class);

    private ActionBar worldBorderActionBar;

    private Map<UUID, List<Location>> lastFakeBorderBlocks = new HashMap<>();

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
            && altWarningMode.getValue() != PlatformTarget.NONE) {
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
        if (event.getType() == UpdateType.FAST) {
            if (borderWallEnabled.getValue() == PlatformTarget.NONE) {
                lastFakeBorderBlocks.forEach((u, l) -> {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) {
                        l.forEach(location -> {
                            uhc.fakeBlockManager.resetFakeBlock(p, location);
                        });
                    }
                });
                lastFakeBorderBlocks.clear();
            } else {
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (borderWallEnabled.getValue() == PlatformTarget.ALL || (
                        borderWallEnabled.getValue() == PlatformTarget.BEDROCK_ONLY
                            && FloodgateWrapper.getFloodgate()
                            .isFloodgatePlayer(p.getUniqueId()))) {
                        showFakeBorder(p);
                    } else {
                        List<Location> lastBlocks = lastFakeBorderBlocks.remove(p.getUniqueId());
                        if (lastBlocks != null) {
                            lastBlocks.forEach(l -> uhc.fakeBlockManager.resetFakeBlock(p, l));
                        }
                    }
                });
            }
        }
    }

    private double getDistanceSquaredToWorldBorder(Player player) {
        Location playerPos = player.getLocation();
        World world = playerPos.getWorld();
        Location wbCenter = world.getWorldBorder().getCenter();

        double locX = (world.getWorldBorder().getSize() / 2.0) + wbCenter.getX();
        double locZ = (world.getWorldBorder().getSize() / 2.0) + wbCenter.getZ();

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

        return Math.min(
            Math.min(distanceSouth, Math.min(distanceNorth, distanceEast)), distanceWest);
    }

    private ClosestWorldborder getClosestWorldBorderDirection(Player player) {
        Location playerPos = player.getLocation();
        World world = playerPos.getWorld();
        Location wbCenter = world.getWorldBorder().getCenter();

        double locX = (world.getWorldBorder().getSize() / 2.0) + wbCenter.getX();
        double locZ = (world.getWorldBorder().getSize() / 2.0) + wbCenter.getZ();

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

        if (distanceSouth < distanceNorth && distanceSouth < distanceEast
            && distanceSouth < distanceWest) {
            return ClosestWorldborder.SOUTH;
        } else if (distanceNorth < distanceEast && distanceNorth < distanceWest) {
            return ClosestWorldborder.NORTH;
        } else if (distanceEast < distanceWest) {
            return ClosestWorldborder.EAST;
        }
        return ClosestWorldborder.WEST;
    }

    private void updateWarningActionbar(Player player) {
        Location playerPos = player.getLocation();
        World world = playerPos.getWorld();
        if (world != null && world.getWorldBorder().getWarningDistance() > 0) {
            boolean outsideWorldborder = false;
            double[] border = worldborderLoc(world);
            if (Math.abs(playerPos.getX()) > border[0] || Math.abs(playerPos.getZ()) > border[1]) {
                outsideWorldborder = true;
            }

            double distance = Math.sqrt(getDistanceSquaredToWorldBorder(player));
            if (distance < world.getWorldBorder().getWarningDistance() || outsideWorldborder) {
                double distancePercent =
                    1 - (distance / world.getWorldBorder().getWarningDistance());
                if (outsideWorldborder) {
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
     * Show the player a fake border made of glass
     *
     * @param player The player
     */
    public void showFakeBorder(Player player) {
        List<Location> lastBlocks = this.lastFakeBorderBlocks.computeIfAbsent(player.getUniqueId(),
            u -> new ArrayList<>());
        if (getDistanceSquaredToWorldBorder(player) > Math.pow(20, 2) || player.getWorld().getWorldBorder().getSize() < 5) {
            if (lastBlocks.size() > 0) {
                lastBlocks.forEach(b -> this.uhc.fakeBlockManager.resetFakeBlock(player, b));
            }
            return;
        }
        List<Location> newBlocks = new ArrayList<>();
        double[] wbLoc = worldborderLoc(player.getWorld());
        wbLoc[0] = Math.round(wbLoc[0]);
        wbLoc[1] = Math.round(wbLoc[1]);
        ClosestWorldborder closestWorldborder = getClosestWorldBorderDirection(player);
        int offsetX = 5;
        int offsetY = 5;
        int offsetZ = 5;
        for (int oX = offsetX * -1; oX < offsetX; oX++) {
            for (int oY = offsetY * -1; oY < offsetY; oY++) {
                for (int oZ = offsetZ * -1; oZ < offsetZ; oZ++) {
                    Location l = player.getLocation().getBlock().getLocation();
                    l.add(oX, oY, oZ);

                    switch (closestWorldborder) {
                        case NORTH:
                            l.setZ((Math.abs(wbLoc[0]) * -1) - 1);
                            break;
                        case SOUTH:
                            l.setZ(Math.abs(wbLoc[0]));
                            break;
                        case EAST:
                            l.setX(Math.abs(wbLoc[1]));
                            break;
                        case WEST:
                            l.setX((Math.abs(wbLoc[1]) * -1) - 1);
                            break;
                    }
                    if (l.distanceSquared(player.getLocation()) < Math.pow(5, 2)) {
                        if (Math.abs(player.getLocation().getX()) < wbLoc[0]
                            && Math.abs(player.getLocation().getZ()) < wbLoc[1]) {
                            newBlocks.add(l);
                        }
                    }
                }
            }
        }
        lastBlocks.stream().filter(l -> !newBlocks.contains(l))
            .forEach(l -> uhc.fakeBlockManager.resetFakeBlock(player, l));
        newBlocks.stream().filter(l -> !lastBlocks.contains(l))
            .forEach(l -> uhc.fakeBlockManager.setFakeBlock(player, l, borderWall.getValue()));
        this.lastFakeBorderBlocks.put(player.getUniqueId(), newBlocks);
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
        return worldborderLoc(this.game.getUHCWorld());
    }

    public double[] worldborderLoc(World world) {
        WorldBorder wb = world.getWorldBorder();
        Location l = wb.getCenter();
        double locX = (wb.getSize() / 2) + l.getX();
        double locZ = (wb.getSize() / 2) + l.getZ();
        return new double[]{locX, locZ};
    }

    public enum PlatformTarget {
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
