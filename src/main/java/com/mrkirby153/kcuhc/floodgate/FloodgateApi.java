package com.mrkirby153.kcuhc.floodgate;

import java.util.UUID;

public class FloodgateApi implements FloodgateApiImpl {

    private final org.geysermc.floodgate.api.FloodgateApi api;

    public FloodgateApi() {
        this.api = org.geysermc.floodgate.api.FloodgateApi.getInstance();
    }

    @Override
    public boolean isFloodgatePlayer(UUID uuid) {
        return this.api.isFloodgatePlayer(uuid);
    }
}
