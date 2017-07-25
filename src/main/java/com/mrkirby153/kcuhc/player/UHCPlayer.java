package com.mrkirby153.kcuhc.player;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

/**
 * Stores data about the player
 */
public class UHCPlayer {

    private static HashMap<UUID, UHCPlayer> players = new HashMap<>();

    private final UUID uuid;

    public UHCPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Resolves a {@link Player} to a {@link UHCPlayer}
     *
     * @param player The player to resolve
     * @return The {@link UHCPlayer}
     */
    public static UHCPlayer getPlayer(Player player) {
        if (!players.containsKey(player.getUniqueId())) {
            players.put(player.getUniqueId(), new UHCPlayer(player.getUniqueId()));
        }
        return players.get(player.getUniqueId());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UHCPlayer && ((UHCPlayer) obj).uuid.equals(uuid);
    }

    /**
     * Gets the UUID of the player
     *
     * @return The player's UUID
     */
    public UUID getUuid() {
        return uuid;
    }
}
