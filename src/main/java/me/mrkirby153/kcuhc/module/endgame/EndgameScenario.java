package me.mrkirby153.kcuhc.module.endgame;

import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.team.TeamSpectator;
import me.mrkirby153.kcuhc.team.UHCTeam;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class EndgameScenario extends UHCModule {

    public EndgameScenario(Material material, int damage, String name, String description) {
        super(material, damage, name, false, description);
    }

    public abstract void update();

    public void stop(String winner, Color teamColor){
        getPlugin().arena.stop(winner, teamColor);
    }


    public int teamCountLeft() {
        return teamsLeft().size();
    }

    public ArrayList<UHCTeam> teamsLeft() {
        HashSet<UHCTeam> uniqueTeams = new HashSet<>();
        for (Player p : getPlugin().arena.players(false)) {
            UHCTeam team = getPlugin().teamHandler.getTeamForPlayer(p);
            if (team == null)
                continue;
            if (team instanceof TeamSpectator)
                continue;
            uniqueTeams.add(team);
        }
        for(OfflinePlayer p : getPlugin().arena.getDisconnectedPlayers()){
            UHCTeam team = getPlugin().teamHandler.getTeamForPlayer(p);
            if(team == null)
                continue;
            if(team instanceof TeamSpectator)
                continue;
            uniqueTeams.add(team);
        }
        return new ArrayList<>(uniqueTeams);
    }
}
