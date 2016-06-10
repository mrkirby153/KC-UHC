package me.mrkirby153.kcuhc.discord.commands;

import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.kcuhc.team.UHCTeam;

import java.util.*;

public class AssignTeams extends DiscordCommand{

    private HashSet<UHCTeam> teams = new HashSet<>();

    public AssignTeams(Collection<UHCTeam> teams){
        super("assignTeams");
        this.teams.addAll(teams);
    }

    @Override
    public void process(ByteArrayDataOutput out) {
        HashMap<UUID, String> teams = new HashMap<>();
        for(UHCTeam t : this.teams){
            for(UUID u : t.getPlayers()){
                teams.put(u, t.getName());
            }
        }
        out.writeInt(teams.size());
        for(Map.Entry<UUID, String> e : teams.entrySet()){
            out.writeUTF(e.getKey().toString());
            out.writeUTF(e.getValue());
        }
    }
}
