package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.utils.UtilChat;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class BorderBumper implements Runnable {

    private HashMap<UUID, Long> bumperCooldown = new HashMap<>();

    private static final double BUMP_POWER = 0.25;

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (bumperCooldown.get(player.getUniqueId()) != null) {
                if (System.currentTimeMillis() < bumperCooldown.get(player.getUniqueId()))
                    return;
            }
            double borderBounds = (player.getWorld().getWorldBorder().getSize() / 2) - 0.25;
            if(borderBounds < 10)
                return;
            if (Math.abs(player.getLocation().getX()) > borderBounds || Math.abs(player.getLocation().getZ()) > borderBounds) {
                bumpPlayer(player, player.getWorld().getWorldBorder().getCenter(), player.getWorld().getWorldBorder().getSize());
                bumperCooldown.put(player.getUniqueId(), System.currentTimeMillis() + 500);
            }
        }
    }


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
        player.sendMessage(UtilChat.message(ChatColor.BOLD + "Stay inside the world border!".toUpperCase()));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BASS, 1F, 1F);
    }


    enum Direction {
        UNKNOWN,
        POSITIVE_Z,
        NEGATIVE_Z,
        POSITIVE_X,
        NEGATIVE_X;
    }

}
