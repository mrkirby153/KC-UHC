package me.mrkirby153.kcuhc.module.endgame;

import me.mrkirby153.kcuhc.team.LoneWolfTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcutils.C;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Objects;
import java.util.stream.Collectors;

public class TeamsEndgame extends EndgameScenario {

    public TeamsEndgame() {
        super(Material.GOLD_AXE, 0, "Last Team Standing", "Game ends when there's one team left");
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(C.m("Endgame", "Last Team standing wins!").toLegacyText());
    }

    @Override
    public void update() {
        if (teamCountLeft() <= 1) {
            if (teamsLeft().size() > 0) {
                UHCTeam team = teamsLeft().get(0);
                if (team instanceof LoneWolfTeam) {
                    if (team.getPlayers().size() <= 1) {
                        stop(team.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList()).get(0).getDisplayName(), team.toColor());
                    }
                } else {
                    stop(team.getFriendlyName(), team.toColor());
                }
            } else {
                getPlugin().arena.stop("Nobody", Color.WHITE);
            }
        }
    }
}
