package com.mrkirby153.kcuhc.fakeblock;

import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class FakeBlockManager implements Listener {

    private static final double DISTANCE_SQ_TICK_UPDATE = Math.pow(10, 2);
    private static final double DISTANCE_SQ_FAST_UPDATE = Math.pow(20, 2);

    private Set<FakeBlock> globalFakeBlocks = new HashSet<>();
    private Map<UUID, Set<FakeBlock>> fakeBlocks = new HashMap<>();
    private Map<UUID, Set<Location>> pendingResets = new HashMap<>();


    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.TICK) {
            // Update blocks that users are within 10 blocks of
            Bukkit.getOnlinePlayers().forEach(p -> {
                fakeBlocks.getOrDefault(p.getUniqueId(), Set.of()).forEach(block -> {
                    if (block.location.distanceSquared(p.getLocation()) < DISTANCE_SQ_TICK_UPDATE) {
                        sendFakeBlock(p, block);
                    }
                });
            });
        }
        if (event.getType() == UpdateType.FAST) {
            // Update blocks that users are within 20 blocks of
            Bukkit.getOnlinePlayers().forEach(p -> {
                fakeBlocks.getOrDefault(p.getUniqueId(), Set.of()).forEach(block -> {
                    if (block.location.distanceSquared(p.getLocation()) < DISTANCE_SQ_FAST_UPDATE) {
                        sendFakeBlock(p, block);
                    }
                });
            });
        }
        if (event.getType() == UpdateType.SLOW) {
            // Reset blocks that need to be reset
            resetBlocks();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        getFakeBlocks(event.getPlayer()).addAll(globalFakeBlocks);
    }


    public void setFakeBlock(Player player, Location location, Material material) {
        setFakeBlock(location, player, material.createBlockData());
    }

    public void setFakeBlock(Location location, Material material) {
        setFakeBlock(location, null, material.createBlockData());
    }

    public void resetFakeBlock(Player player, Location location) {
        Set<FakeBlock> blocks = player != null ? getFakeBlocks(player) : globalFakeBlocks;
        blocks.removeIf(b -> b.getLocation().equals(location));
        if (player != null) {
            getPendingReset(player).add(location);
        } else {
            Bukkit.getOnlinePlayers().forEach(p -> {
                getFakeBlocks(p).removeIf(b -> b.getLocation().equals(location));
                getPendingReset(p).add(location);
            });
        }
    }

    public void setFakeBlock(Location location, Player player, BlockData blockData) {
        Set<FakeBlock> blocks = player != null ? getFakeBlocks(player) : globalFakeBlocks;
        blocks.add(new FakeBlock(location, blockData));
    }

    public void setFakeBlock(Location location, BlockData blockData) {
        setFakeBlock(location, null, blockData);
    }

    private Set<FakeBlock> getFakeBlocks(Player player) {
        return fakeBlocks.computeIfAbsent(player.getUniqueId(), u -> new HashSet<>());
    }

    private Set<Location> getPendingReset(Player player) {
        return pendingResets.computeIfAbsent(player.getUniqueId(), u -> new HashSet<>());
    }

    private void sendFakeBlock(Player player, FakeBlock block) {
        player.sendBlockChange(block.getLocation(), block.getBlockData());
    }

    private void sendResetBlock(Player player, Location location) {
        player.sendBlockChange(location, location.getBlock().getBlockData());
    }

    private void resetBlocks() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            Set<Location> pendingReset = pendingResets.remove(p.getUniqueId());
            if (pendingReset != null) {
                pendingReset.forEach(l -> sendResetBlock(p, l));
            }
        });
    }


    private class FakeBlock {

        private final BlockData blockData;
        private final Location location;

        public FakeBlock(Location location, BlockData data) {
            this.location = location;
            this.blockData = data;
        }

        public BlockData getBlockData() {
            return blockData;
        }

        public Location getLocation() {
            return location;
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockData, location);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FakeBlock fakeBlock = (FakeBlock) o;
            return Objects.equals(location, fakeBlock.location);
        }
    }
}
