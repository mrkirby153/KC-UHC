package com.mrkirby153.kcuhc.floodgate;

import java.util.UUID;

public class NoOpFloodgateApi implements FloodgateApiImpl {

    @Override
    public boolean isFloodgatePlayer(UUID uuid) {
        return false; // Floodgate is not installed
    }
}
