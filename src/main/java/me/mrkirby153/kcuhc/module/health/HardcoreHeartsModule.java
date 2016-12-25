package me.mrkirby153.kcuhc.module.health;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Material;

public class HardcoreHeartsModule extends UHCModule {

    private final HardcoreHeartsListener HARDCORE_HEARTS_LISTENER = new HardcoreHeartsListener();

    public HardcoreHeartsModule() {
        super(Material.DOUBLE_PLANT, 4, "Hardcore Hearts", false, "Shows hardcore hearts");
    }

    @Override
    public void onEnable() {
        ProtocolLibrary.getProtocolManager().addPacketListener(HARDCORE_HEARTS_LISTENER);
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListener(HARDCORE_HEARTS_LISTENER);
    }

    class HardcoreHeartsListener extends PacketAdapter{
        HardcoreHeartsListener(){
            super(UHC.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Server.LOGIN);
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            if (event.getPacketType().equals(PacketType.Play.Server.LOGIN)) {
                event.getPacket().getBooleans().write(0, true);
            }
        }
    }
}
