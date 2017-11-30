package com.mrkirby153.kcuhc.module.health;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class HardcoreHeartsModule extends UHCModule {

    private HardcoreHeartsListener listener;
    private UHC uhc;

    @Inject
    public HardcoreHeartsModule(UHC uhc) {
        super("Hardcore Hearts", "Show hardcore hearts", Material.DOUBLE_PLANT, 4);
        this.uhc = uhc;
    }

    @Override
    public void onLoad() {
        ProtocolLibrary.getProtocolManager()
            .addPacketListener(listener = new HardcoreHeartsListener(uhc));
    }

    @Override
    public void onUnload() {
        if (listener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(listener);
        }
    }

    class HardcoreHeartsListener extends PacketAdapter {

        HardcoreHeartsListener(JavaPlugin plugin) {
            super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.LOGIN);
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            if (event.getPacketType().equals(PacketType.Play.Server.LOGIN)) {
                event.getPacket().getBooleans().write(0, true);
            }
        }
    }
}
