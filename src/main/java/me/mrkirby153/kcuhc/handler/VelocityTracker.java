package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.Module;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class VelocityTracker extends Module<UHC> implements Runnable, Listener {

    private HashMap<UUID, PlayerVelocity> velocities = new HashMap<>();

    private boolean debug;

    public VelocityTracker(UHC plugin, boolean debug) {
        super("Velocity Tracker", "1.0", plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.debug = debug;
    }

    public VelocityTracker(UHC plugin) {
        this(plugin, false);
    }

    public Vector getVelocity(Player player) {
        PlayerVelocity vel = velocities.get(player.getUniqueId());
        return new Vector(vel.motX, vel.motY, vel.motZ);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void playerLogin(PlayerLoginEvent event) {
        velocities.put(event.getPlayer().getUniqueId(), new PlayerVelocity(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerLogout(PlayerQuitEvent event) {
        velocities.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void run() {
        if (debug)
            System.out.println("/////////////");
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID u = p.getUniqueId();
            PlayerVelocity velocity = velocities.computeIfAbsent(u, k -> new PlayerVelocity(p));
            double currX, currY, currZ;
            Location currLoc = p.getLocation();
            currX = currLoc.getX();
            currY = currLoc.getY();
            currZ = currLoc.getZ();

            velocity.motX = currX - velocity.lastX;
            velocity.motY = currY - velocity.lastY;
            velocity.motZ = currZ - velocity.lastZ;

            if (debug) {
                System.out.println(p.getName());
                System.out.println("\tPOS: " + currX + ", " + currY + ", " + currZ);
                System.out.println("\tVEL: " + velocity.motX + ", " + velocity.motY + ", " + velocity.motZ);
            }

            velocity.lastX = currX;
            velocity.lastY = currY;
            velocity.lastZ = currZ;
        }
    }

    @Override
    protected void init() {
        scheduleRepeating(this, 0L, 1L);
        registerListener(this);
    }


    private class PlayerVelocity {

        public double motX, motY, motZ;

        public double lastX, lastY, lastZ;

        public UUID forPlayer;

        public PlayerVelocity(Player player) {
            this.forPlayer = player.getUniqueId();
        }

    }
}
