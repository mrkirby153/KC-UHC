package com.mrkirby153.kcuhc.module.respawner;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;

public class TeamRespawnModule extends UHCModule {

    private RespawnerCommand respawnerCommand;

    public TeamRespawnStructure structure;

    @Inject
    public TeamRespawnModule(UHC uhc) {
        super("Team Respawn", "Creates a structure to respawn teammates", Material.NETHER_STAR);
        respawnerCommand = new RespawnerCommand(this);
    }

    @Override
    public void onLoad() {
        System.out.println("Loading");
        UHC.getCommandManager().registerCommand(respawnerCommand, true);
    }

    @Override
    public void onUnload() {
        UHC.getCommandManager().unregisterCommand(respawnerCommand);
    }

    @EventHandler
    public void onTick(UpdateEvent e) {
        if (e.getType() == UpdateType.TICK) {
            if (structure != null) {
                structure.tick();
            }
        }
    }
}
