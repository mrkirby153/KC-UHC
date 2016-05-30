package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.arena.UHCArena;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class BorderBumper implements Runnable {

    private HashMap<UUID, Long> bumperCooldown = new HashMap<>();

    @Override
    public void run() {
        if(UHC.arena.currentState() != UHCArena.State.RUNNING)
            return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (bumperCooldown.get(player.getUniqueId()) != null) {
                if (System.currentTimeMillis() < bumperCooldown.get(player.getUniqueId()))
                    return;
            }
            double borderBounds = player.getWorld().getWorldBorder().getSize() / 2;
            if (Math.abs(player.getLocation().getX()) > borderBounds || Math.abs(player.getLocation().getZ()) > borderBounds) {
                // Bump them towards spawn?
                Vector playerVector = player.getLocation().toVector();
                Vector wbCenter = player.getWorld().getWorldBorder().getCenter().toVector();

                double distanceTo = wbCenter.clone().setY(0).subtract(playerVector.clone().setY(0)).length();
                System.out.println(distanceTo);
                if(distanceTo < 10)
                    return;

                Vector vector = wbCenter.subtract(playerVector).setY(0).normalize();
                vector.setY(0.25);
                vector.multiply(0.75);
                player.setVelocity(vector);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BASS, 1F, 1F);
                player.spigot().sendMessage(UtilChat.generateFormattedChat("STAY INSIDE OF THE WORLD BORDER!", ChatColor.WHITE, 8));
                bumperCooldown.put(player.getUniqueId(), System.currentTimeMillis() + 500);
            }
        }
    }

}
