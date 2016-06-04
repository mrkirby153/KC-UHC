package me.mrkirby153.kcuhc.arena;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UUIDFetcher;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
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
        HashSet<String> users = new HashSet<>();
        for (String u : (ArrayList<String>) data.get("players")) {
            Pattern uuidPattern = Pattern.compile("[A-Za-z0-9]{8}(\\-?[A-Za-z0-9]{4}\\-?){3}[A-Za-z0-9]{12}");
            UUID uuid;
            if (uuidPattern.matcher(u).find()) {
                uuid = UUID.fromString(u);
            } else {
                // Look up username
                users.add(u);
                uuid = Bukkit.getOfflinePlayer(u).getUniqueId();
            }
            if (!users.isEmpty()) {
                System.out.println("Performing UUID lookup");
                Bukkit.getServer().getScheduler().runTaskAsynchronously(UHC.plugin, new UUIDLookupThread(upt, users));
            }
            if (uuid != null)
                upt.addUUID(uuid);
        }
        return upt;
    }


    private static class UUIDLookupThread implements Runnable {

        private HashSet<String> names;

        private UHCPlayerTeam team;

        public UUIDLookupThread(UHCPlayerTeam team, HashSet<String> names) {
            this.names = names;
            this.team = team;
        }

        @Override
        public void run() {
            ArrayList<String> names = new ArrayList<>();
            names.addAll(this.names);
            UUIDFetcher fetcher = new UUIDFetcher(names, true);
            Map<String, UUID> fetched = fetcher.call();
            for (Map.Entry<String, UUID> e : fetched.entrySet()) {
                System.out.println("Found UUID for " + e.getKey() + " (" + e.getValue() + ")");
                team.addUUID(e.getValue());
            }
            YamlConfiguration config = new YamlConfiguration();
            config.set(team.getName(), team);
            TeamHandler.saveTeam(team);
        }
    }
}
