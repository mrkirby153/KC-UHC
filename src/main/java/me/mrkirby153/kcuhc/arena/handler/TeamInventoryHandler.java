package me.mrkirby153.kcuhc.arena.handler;

import me.mrkirby153.kcuhc.team.UHCTeam;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class TeamInventoryHandler {

    private HashMap<UHCTeam, Inventory> teamInventories = new HashMap<>();

    public TeamInventoryHandler() {
    }

    public void dropInventory(UHCTeam team, Location location) {
        Inventory inv = teamInventories.remove(team);
        if (inv == null)
            return;
        for (ItemStack i : inv.getContents()) {
            if (i != null)
                location.getWorld().dropItemNaturally(location, i);
        }
    }

    public Inventory getInventory(UHCTeam team) {
        Inventory inv = teamInventories.get(team);
        if (inv == null) {
            String name = team.getFriendlyName();
            if (name == null)
                name = WordUtils.capitalizeFully(team.getTeamName().replace('_', ' '));
            teamInventories.put(team, inv = Bukkit.createInventory(null, 9 * 3, "Team Inventory: " + name));
        }
        return inv;
    }

    public void reset(){
        teamInventories.clear();
    }
}
