package me.mrkirby153.kcuhc.team;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.player.LoneWolfModule;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class LoneWolfTeam extends UHCTeam {

    public LoneWolfTeam() {
        super("lonewolf", ChatColor.DARK_GRAY);
        setFriendlyFire(true);
        setSeeInvisible(false);
    }

    @Override
    public String getFriendlyName() {
        return "Lone Wolves";
    }

    @Override
    public void onJoin(Player player) {
        UHC.getInstance().arena.addPlayer(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setDisplayName(getColor() + player.getName() + ChatColor.RESET);
    }

    @Override
    public void onLeave(Player player) {
        player.setDisplayName(player.getName());
        ModuleRegistry.getLoadedModule(LoneWolfModule.class).ifPresent(m -> m.removeLoneWolf(player));
    }
}
