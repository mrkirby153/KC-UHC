package com.mrkirby153.kcuhc.module.respawner;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TeamRespawnStructure {

    public static final int STRUCTURE_SIZE = 1;
    public static final int STRUCTURE_HEIGHT = 2;

    private static final int RESPAWN_TIME_TICKS = 120 * 20;
    private static final int COOLDOWN_TIME_TICKS = 300 * 20;
    public double r = 1;
    double bounds = 0;
    private Location center;
    private long ticksRemaining = -1L;
    private long totalTicks = -1L;
    private Phase phase = Phase.DEACTIVATED;
    private double t = 0.0;
    private Inventory inventory = Bukkit.createInventory(null, 9, "Team Respawner");
    private List<WeakReference<Player>> observers = new ArrayList<>();
    private UHC plugin;

    public TeamRespawnStructure(UHC plugin, Location location) {
        this.center = location;
        this.center.setPitch(0);
        this.center.setYaw(0);
        this.center.setX(this.center.getBlockX());
        this.center.setY(this.center.getBlockY());
        this.center.setZ(this.center.getBlockZ());
        this.plugin = plugin;

        // Fill the slots
    }

    public boolean isActive() {
        return phase == Phase.RESPAWNING;
    }

    public Inventory getInventory() {
        int[] filledSlots = {0, 1, 2, 3, 5, 6, 7, 8};
        for (int i : filledSlots) {
            ItemStack s = new ItemFactory(Material.GRAY_STAINED_GLASS_PANE)
                .name(ChatColor.GOLD + "" + ChatColor.BOLD + "Insert a soul vial").construct();
            this.inventory.setItem(i, s);
        }
        return this.inventory;
    }

    public void setBeaconBase(Material material) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int newX = center.getBlockX() + i;
                int newZ = center.getBlockZ() + j;
                Location newLoc = new Location(center.getWorld(), newX, center.getBlockY(), newZ);
                newLoc.getBlock().setType(material);
            }
        }
    }

    public void buildStructure() {
        setBeaconBase(Material.IRON_BLOCK);
        this.center.getBlock().setType(Material.STONE);
        // Set the beacon
        this.center.clone().add(0, 1, 0).getBlock().setType(Material.BEACON);
        // Set the signs
        setWallsign(this.center.clone().add(1, 1, 0), BlockFace.EAST);
        setWallsign(this.center.clone().add(-1, 1, 0), BlockFace.WEST);
        setWallsign(this.center.clone().add(0, 1, 1), BlockFace.SOUTH);
        setWallsign(this.center.clone().add(0, 1, -1), BlockFace.NORTH);
        // Set the chest
        this.center.clone().add(0, 2, 0).getBlock().setType(Material.CHEST);
    }

    private void setWallsign(Location l, BlockFace facing) {
        Block b = l.getBlock();
        b.setType(Material.WALL_SIGN);
        WallSign ws = (WallSign) b.getBlockData();
        ws.setFacing(facing);
        b.setBlockData(ws);
    }

    private void setAllSignTexts(String[] lines) {
        setSignText(this.center.clone().add(1, 1, 0), lines);
        setSignText(this.center.clone().add(-1, 1, 0), lines);
        setSignText(this.center.clone().add(0, 1, 1), lines);
        setSignText(this.center.clone().add(0, 1, -1), lines);
    }

    private String[] getStatusMessage() {
        String[] lines = new String[4];
        lines[1] = "Soul Monument";
        switch (this.phase) {
            case DEACTIVATED:
                lines[2] = ChatColor.GRAY + "DEACTIVATED";
                break;
            case IDLE:
                lines[2] = ChatColor.DARK_GRAY + "Waiting...";
                break;
            case RESPAWNING:
                lines[0] = "Respawning...";
                lines[1] = getTimeRemaining();

                double passed =
                    ((double) (this.totalTicks - this.ticksRemaining)) / this.totalTicks;
                int hashesToDisplay = (int) Math.floor((passed) * 13);
                StringBuilder s = new StringBuilder("[");
                for (int i = 1; i <= 13; i++) {
                    s.append((i <= hashesToDisplay) ? "#" : " ");
                }
                s.append("]");
                lines[2] = s.toString();
                Player p = SoulVialHandler.getInstance().getSoulVialContents(this.inventory.getItem(4));
                lines[3] = p != null? p.getName() : "";
                break;
            case RECHARGING:
                lines[2] = getTimeRemaining();
                break;
        }
        return lines;
    }

    public void setTicksLeft(int ticksLeft) {
        this.ticksRemaining = this.totalTicks = ticksLeft;
    }

    public void tick() {
        updateParticles();
        updateIdleParticles();
        checkRespawnItem();
        if (this.ticksRemaining != -1) {
            this.ticksRemaining--;
        }
        setAllSignTexts(getStatusMessage());
        switch (this.phase) {
            case RESPAWNING:
                if (this.ticksRemaining <= 60) {
                    this.r -= (3) / 60.0;
                }
                if (this.ticksRemaining <= 0) {
                    doRespawn();
                    System.out.println("Respawn structure hit 0 ticks! Respawning team");
                    this.phase = Phase.RECHARGING;
                    this.setTicksLeft(COOLDOWN_TIME_TICKS);
                    this.deactivateBeacon();
                    this.center.clone().add(0, 2, 0).getBlock().setType(Material.AIR);
                    this.center.getWorld().playSound(this.center, Sound.ENTITY_ZOMBIE_VILLAGER_CURE,
                        SoundCategory.MASTER, 1.0F, 1.0F);
                    this.center.getWorld()
                        .spawnParticle(Particle.EXPLOSION_HUGE, this.center.clone().add(0, 2, 0), 1,
                            0, 0, 0, 0);
                }
            case RECHARGING:
                if (this.ticksRemaining <= 0) {
                    System.out.println("Respawn structure cooled down");
                    this.phase = Phase.IDLE;
                    this.center.clone().add(0, 2, 0).getBlock().setType(Material.CHEST);
                }
        }
    }

    private void displayParticles(double x, double y, double z) {
        if (bounds < 1.5) {
            bounds += 0.1;
        }
        for (double y1 = y - bounds; y1 <= y + bounds; y1 += 0.2) {
            this.center.add(x, y1, z);
            this.center.getWorld().spawnParticle(Particle.FLAME, this.center, 50, 0, 0, 0, 0.0);
            this.center.subtract(x, y1, z);
        }
    }

    private void updateParticles() {
        // Display a circling ring
        if (this.phase != Phase.RESPAWNING) {
            t = 0;
            bounds = 0;
            return;
        }
        t += Math.PI / 8;
        double x = r * Math.cos(t);
        double z = r * Math.sin(t);
        double y = 2.25;
        x += 0.5;
        z += 0.5;
        displayParticles(x, y, z);
    }

    private void updateIdleParticles() {
        if (this.phase != Phase.IDLE) {
            return;
        }
        this.center.add(0.5, 2, 0.5);
        this.center.getWorld()
            .spawnParticle(Particle.PORTAL, this.center, 10, 0.25, 0.25, 0.25, 0.0);
        this.center.subtract(0.5, 2, 0.5);
    }

    private void setSignText(Location l, String[] text) {
        Block b = l.getBlock();
        if (b.getType() != Material.WALL_SIGN && b.getType() != Material.SIGN) {
            return;
        }
        Sign s = (Sign) b.getState();
        for (int i = 0; i < text.length; i++) {
            s.setLine(i, text[i]);
        }
        s.update();
    }

    private String getTimeRemaining() {
        long secondsLeft = this.ticksRemaining / 20;
        double minutesLeft = Math.floor((double) secondsLeft / 60.0);
        secondsLeft -= minutesLeft * 60;
        String t = "";
        if (minutesLeft < 10) {
            t += "0";
        }
        t += (int) minutesLeft;
        t += ":";
        if (secondsLeft < 10) {
            t += "0";
        }
        t += secondsLeft;
        return t;
    }

    public void activateBeacon() {
        this.center.getBlock().setType(Material.IRON_BLOCK);
    }

    public void deactivateBeacon() {
        this.center.getBlock().setType(Material.STONE);
    }

    public void startRespawn() {
        if (this.phase != Phase.IDLE) {
            throw new IllegalStateException("Respawn structure not in correct state");
        }
        this.activateBeacon();
        this.setTicksLeft(RESPAWN_TIME_TICKS);
        this.phase = Phase.RESPAWNING;
        this.r = 3;
        System.out.println("Respawn structure started respawn!");
    }

    public Location getCenter() {
        return this.center;
    }

    public Phase getPhase() {
        return this.phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public void removeObserver(Player player) {
        this.observers.removeIf(wr -> wr.get() == player);
    }

    public void addObserver(Player player) {
        WeakReference<Player> wr = new WeakReference<>(player);
        this.observers.add(wr);
    }

    public List<Player> getObservers() {
        return this.observers.stream().map(WeakReference::get).filter(Objects::nonNull).collect(
            Collectors.toList());
    }

    private void checkRespawnItem() {
        if (this.phase != Phase.IDLE) {
            return;
        }
        ItemStack is = this.inventory.getItem(4);
        if (SoulVialHandler.getInstance().isSoulVial(is)) {
            getObservers().forEach(Player::closeInventory);
            startRespawn();
        }
    }

    private void doRespawn() {
        ItemStack vial = this.inventory.getItem(4);
        Player p = SoulVialHandler.getInstance().getSoulVialContents(vial);
        if (p == null) {
            System.out.println("Attempting to respawn a player that no longer exists!");
            return;
        }
        UHCTeam team = SoulVialHandler.getInstance().getTeam(vial);
        ScoreboardTeam current = this.plugin.getGame().getTeam(p);
        if (current instanceof SpectatorTeam) {
            current.removePlayer(p);
            System.out.println("Removing player from spectators team");
        }
        if (team != null) {
            System.out.println("Re-Adding " + p.getName() + " to " + team.getTeamName());
            team.addPlayer(p);
        } else {
            System.out.println("Not adding to team as they weren't a part of one");
        }
        p.teleport(this.center.clone().add(0.5, 2, 0.5));
        this.inventory.setItem(4, null); // Remove the item
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.spigot().sendMessage(Chat.message("Respawn", "{player} has been respawned", "{player}", p.getName()));
        });
    }

    public enum Phase {
        DEACTIVATED,
        IDLE,
        RESPAWNING,
        RECHARGING
    }
}
