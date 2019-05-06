package com.mrkirby153.kcuhc.module.respawner;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitTask;

public class TeamRespawnModule extends UHCModule {

    private RespawnerCommand respawnerCommand;

    public TeamRespawnStructure structure;

    private UHC plugin;
    private BukkitTask bt;

    @Inject
    public TeamRespawnModule(UHC uhc) {
        super("Team Respawn", "Creates a structure to respawn teammates", Material.NETHER_STAR);
        UHC.getCommandManager().registerCommand(respawnerCommand = new RespawnerCommand(this));

        this.plugin = uhc;
    }

    @Override
    public void onLoad() {
        System.out.println("Loading");
        bt = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            if (structure != null) {
                structure.tick();
            }
        }, 0, 2);
    }

    @Override
    public void onUnload() {
        if (bt != null) {
            bt.cancel();
        }
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
