package me.mrkirby153.kcuhc.module.endgame;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.dimension.EndModule;
import me.mrkirby153.kcuhc.module.dimension.NetherModule;
import me.mrkirby153.kcuhc.team.LoneWolfTeam;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.TeamSpectator;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerPortalEvent;

public class SpaceRace extends EndgameScenario {

    public SpaceRace() {
        super(Material.DRAGON_EGG, 0, "Space Race", "First team to the end");
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("First team to the end wins!"));
        ModuleRegistry.getLoadedModule(NetherModule.class).ifPresent(ModuleRegistry::unloadModule);
        ModuleRegistry.getLoadedModule(EndModule.class).ifPresent(ModuleRegistry::unloadModule);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            TeamHandler handler = UHC.getInstance().teamHandler;
            UHCTeam team = handler.getTeamForPlayer(event.getPlayer());
            if (team == null || team instanceof TeamSpectator || team instanceof LoneWolfTeam)
                return;
            Bukkit.getServer().getScheduler().runTaskLater(UHC.getInstance(), () -> {
                stop(team.getFriendlyName(), team.toColor());
            }, 60L);
        }
    }

    @Override
    public void update() {
        if(teamCountLeft() <= 0)
            stop("Nobody", Color.WHITE);
    }
}
