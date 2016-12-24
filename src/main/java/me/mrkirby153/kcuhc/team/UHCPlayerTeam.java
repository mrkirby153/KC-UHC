package me.mrkirby153.kcuhc.team;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class UHCPlayerTeam extends UHCTeam implements ConfigurationSerializable {

    public UHCPlayerTeam(String name, ChatColor color) {
        super(name, color);
        setFriendlyFire(false);
        setSeeInvisible(true);
    }

    @SuppressWarnings("unchecked,deprecation")
    public static UHCPlayerTeam deserialize(Map<String, Object> data) {
        ChatColor co;
        try {
            co = ChatColor.valueOf((String) data.get("color"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown color!");
        }
        UHCPlayerTeam upt = new UHCPlayerTeam((String) data.get("name"), co);
        upt.setFriendlyName((String) data.get("displayName"));
        UHC.getInstance().teamHandler.registerTeam(upt);
        return upt;
    }

    @Override
    public void onJoin(Player player) {
        UHC.getInstance().arena.addPlayer(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setDisplayName(getColor() + player.getName() + ChatColor.RESET);
        if (UHC.getInstance().arena.currentState() == UHCArena.State.RUNNING && UHC.getInstance().arena.getProperties().TEAM_INV_ENABLED.get())
            UHC.getInstance().getServer().getScheduler().runTaskLater(UHC.getInstance(), () -> UHC.getInstance().arena.getTeamInventoryHandler().giveInventoryItem(player), 5L);
    }

    @Override
    public void onLeave(Player player) {
        player.setDisplayName(player.getName());
        UHC.getInstance().arena.getTeamInventoryHandler().takeInventoryItem(player);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", getTeamName());
        data.put("displayName", getFriendlyName());
        data.put("color", getColor().getName().toUpperCase());
        return data;
    }
}
