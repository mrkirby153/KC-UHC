package com.mrkirby153.kcuhc.module.respawner;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;

public class TeamRespawnStructure {

    private static final int RESPAWN_TIME_TICKS = 300;
    private static final int COOLDOWN_TIME_TICKS = 300;

    private Location center;
    private long ticksRemaining = -1L;
    private long totalTicks = -1L;
    private Phase phase = Phase.DEACTIVATED;

    public TeamRespawnStructure(Location location) {
        this.center = location;
    }

    public boolean isActive() {
        return phase == Phase.RESPAWNING;
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
        lines[1] = "Team Respawner";
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
        if (this.ticksRemaining != -1) {
            this.ticksRemaining--;
        }
        setAllSignTexts(getStatusMessage());
        switch (this.phase) {
            case RESPAWNING:
                if (this.ticksRemaining <= 0) {
                    // TODO: 2019-05-05 Actually respawn the team
                    System.out.println("Respawn structure hit 0 ticks! Respawning team");
                    this.phase = Phase.RECHARGING;
                    this.setTicksLeft(COOLDOWN_TIME_TICKS);
                    this.deactivateBeacon();
                    this.center.clone().add(0, 2, 0).getBlock().setType(Material.AIR);
                }
            case RECHARGING:
                if (this.ticksRemaining <= 0) {
                    System.out.println("Respawn structure cooled down");
                    this.phase = Phase.IDLE;
                    this.center.clone().add(0, 2, 0).getBlock().setType(Material.CHEST);
                }
        }
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
        System.out.println("Respawn structure started respawn!");
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public enum Phase {
        DEACTIVATED,
        IDLE,
        RESPAWNING,
        RECHARGING
    }
}
