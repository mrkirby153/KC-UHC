package com.mrkirby153.kcuhc.floodgate;

import java.util.UUID;

public interface FloodgateApiImpl {

    /**
     * Checks if the given player is a floodgate player
     *
     * @param uuid The UUID of the player
     *
     * @return True if the player is a floodgate player
     */
    boolean isFloodgatePlayer(UUID uuid);

}
