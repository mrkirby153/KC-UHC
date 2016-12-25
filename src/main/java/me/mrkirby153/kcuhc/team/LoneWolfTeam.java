package me.mrkirby153.kcuhc.team;

import me.mrkirby153.kcuhc.UHC;
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
    public void onJoin(Player player) {
        UHC.getInstance().arena.addPlayer(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setDisplayName(getColor() + player.getName() + ChatColor.RESET);
    }

    @Override
    public void onLeave(Player player) {
        player.setDisplayName(player.getName());
        UHC.getInstance().loneWolfHandler.removeLoneWolf(player);
    }

    @Override
    public String getFriendlyName() {
        return "Lone Wolves";
    }
}
