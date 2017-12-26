package com.mrkirby153.kcuhc.module.msc.prestige;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PrestigeTeam extends UHCTeam {

    private UUID player;
    private UHC uhc;

    public PrestigeTeam(UHC uhc, UUID player) {
        super("prestige_" + player.toString(), ChatColor.WHITE);
        this.player = player;
        this.uhc = uhc;
        setShowPrefix(true);
    }

    public void update() {
        setPrefixFormat("[%s] ");
        if (!getPlayers().contains(player)) {
            addPlayer(player);
        }
        Player p = Bukkit.getPlayer(this.player);
        if (p == null) {
            return;
        }
        ModuleRegistry.INSTANCE.getLoadedModule(PrestigeModule.class).ifPresent(mod -> {
            setPrefixColor(ChatColor.WHITE);
            setPrefix(mod.getPrestigeMap().getOrDefault(player, 0) + "" + ChatColor.GOLD + "⭐"
                + ChatColor.RESET);
        });
        ScoreboardTeam team = uhc.getGame().getTeam(p);
        if (team == null || team instanceof SpectatorTeam) {
            setColor(ChatColor.WHITE);
        } else {
            setColor(team.getColor());
        }
    }
}
