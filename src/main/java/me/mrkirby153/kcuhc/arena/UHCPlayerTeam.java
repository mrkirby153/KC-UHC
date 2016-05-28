package me.mrkirby153.kcuhc.arena;

import me.mrkirby153.kcuhc.UHC;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Pattern;

public class UHCPlayerTeam extends UHCTeam implements ConfigurationSerializable {

    public UHCPlayerTeam(String name, ChatColor color) {
        super(name, color);
    }

    @Override
    public void onJoin(Player player) {
        UHC.arena.addPlayer(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setDisplayName(getColor() + player.getName() + ChatColor.RESET);
    }

    @Override
    public void onLeave(Player player) {
        player.setDisplayName(player.getName());
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        Collection<String> uuids = new ArrayList<>();
        for (UUID u : getPlayers()) {
            uuids.add(u.toString());
        }
        data.put("name", getName());
        data.put("displayName", getFriendlyName());
        data.put("color", getColor().getName().toUpperCase());
        data.put("players", uuids);
        return data;
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
        TeamHandler.registerTeam((String) data.get("name"), upt);
        for (String u : (ArrayList<String>) data.get("players")) {
            Pattern uuidPattern = Pattern.compile("[A-Za-z0-9]{8}(\\-?[A-Za-z0-9]{4}\\-?){3}[A-Za-z0-9]{12}");
            UUID uuid;
            if (uuidPattern.matcher(u).find()) {
                uuid = UUID.fromString(u);
            } else {
                uuid = Bukkit.getOfflinePlayer(u).getUniqueId();
            }
            upt.addUUID(uuid);
        }
        return upt;
    }
}
