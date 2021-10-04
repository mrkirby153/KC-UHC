package com.mrkirby153.kcuhc.floodgate;

import com.mrkirby153.kcuhc.UHC;

public class FloodgateWrapper {

    private static FloodgateApiImpl instance = null;

    public static FloodgateApiImpl getFloodgate() {
        if (instance == null) {
            try {
                Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                instance = new FloodgateApi();
                UHC.getPlugin(UHC.class).getLogger().info("Using floodgate API");
            } catch (ClassNotFoundException e) {
                instance = new NoOpFloodgateApi();
                UHC.getPlugin(UHC.class).getLogger().info("Using no-op floodgate API");
            }
        }
        return instance;
    }
}
