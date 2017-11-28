package com.mrkirby153.kcuhc.module.worldborder;

import com.mrkirby153.kcuhc.module.UHCModule;
import java.util.HashMap;
import java.util.UUID;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;

public class BorderBumper extends UHCModule {

    private HashMap<UUID, Long> bumperCooldown = new HashMap<>();

    public BorderBumper() {
        super("Worldborder Bumper", "Bump players back inside the worldborder", Material.PISTON_BASE);
    }

    /**
     * Bump a player towards the center of the worldborder
     *
     * @param player   The player to bump
     * @param wbCenter The center of the worldborder
     * @param wbSize   The size of the worldborder
     */
    public static void bumpPlayer(Player player, Location wbCenter, double wbSize) {
        double worldBorderX = Math.abs(wbCenter.getX() + (wbSize / 2)) - 1;
        double worldBorderZ = Math.abs(wbCenter.getZ() + (wbSize / 2)) - 1;

        double distPosX, distNegX, distPosZ, distNegZ;
        Location playerLocation = player.getLocation();
        distPosX = Math.abs(worldBorderX) - playerLocation.getX();
        distNegX = Math.abs(-Math.abs(worldBorderX) - playerLocation.getX());

        distPosZ = Math.abs(worldBorderZ) - playerLocation.getZ();
        distNegZ = Math.abs(-Math.abs(worldBorderZ) - playerLocation.getZ());
        Direction closest;
        double closestPos = Math.min(distPosX, Math.min(distNegX, Math.min(distPosZ, distNegZ)));

        if (closestPos == distPosX)
            closest = Direction.POSITIVE_X;
        else if (closestPos == distNegX)
            closest = Direction.NEGATIVE_X;
        else if (closestPos == distPosZ)
            closest = Direction.POSITIVE_Z;
        else if (closestPos == distNegZ)
            closest = Direction.NEGATIVE_Z;
        else
            closest = Direction.UNKNOWN;
        Vector bumpVector = new Vector(0, 0, 0);
        switch (closest) {
            case POSITIVE_X:
                bumpVector.setX(-1);
                break;
            case NEGATIVE_X:
                bumpVector.setX(1);
                break;
            case POSITIVE_Z:
                bumpVector.setZ(-1);
                break;
            case NEGATIVE_Z:
                bumpVector.setZ(1);
                break;
        }
        bumpVector.multiply(0.75);
        bumpVector.setY(0.1879);
        player.setVelocity(bumpVector);
        player.sendMessage(Chat.INSTANCE.message("Stay inside the worldborder".toUpperCase()).toLegacyText());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BASS, 1F, 1F);
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.TICK)
            return;
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (bumperCooldown.containsKey(p.getUniqueId()))
                if (System.currentTimeMillis() < bumperCooldown.get(p.getUniqueId()))
                    return;
            WorldBorder worldBorder = p.getWorld().getWorldBorder();
            double borderBounds = (worldBorder.getSize() / 2) - 0.25;
            if (borderBounds < 10)
                return;
            if (Math.abs(p.getLocation().getX()) > borderBounds || Math.abs(p.getLocation().getZ()) > borderBounds) {
                bumpPlayer(p, p.getLocation().getWorld().getWorldBorder().getCenter(), worldBorder.getSize());
                bumperCooldown.put(p.getUniqueId(), System.currentTimeMillis() + 500);
            }
        });
    }


    enum Direction {
        UNKNOWN,
        POSITIVE_Z,
        NEGATIVE_Z,
        POSITIVE_X,
        NEGATIVE_X;
    }


}
