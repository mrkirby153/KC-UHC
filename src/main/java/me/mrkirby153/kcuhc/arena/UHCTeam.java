package me.mrkirby153.kcuhc.arena;

import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public abstract class UHCTeam {

    private String name;
    private ChatColor color;

    private String friendlyName;

    private ArrayList<UUID> players = new ArrayList<>();

    public UHCTeam(String name, ChatColor color) {
        this.name = name;
        this.friendlyName = WordUtils.capitalize(name.replace("_", " "));
        this.color = color;
    }

    public void addPlayer(Player player) {
        if (!players.contains(player.getUniqueId()))
            players.add(player.getUniqueId());
/*        TextComponent message = new TextComponent("You are now on team ");
        message.setColor(ChatColor.GREEN);
        TextComponent team = new TextComponent(friendlyName);
        team.setColor(color);
        message.addExtra(team);*/
        player.sendMessage(ChatColor.GREEN + "You are now on team " + color + friendlyName);
        onJoin(player);
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
    }

    public abstract void onJoin(Player player);

    public abstract void onLeave(Player player);

    public boolean onTeam(Player player) {
        return players.contains(player.getUniqueId());
    }

    public ArrayList<UUID> getPlayers() {
        return this.players;
    }

    public String getName() {
        return this.name;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String name) {
        this.friendlyName = name;
    }

    public String getScoreboardName() {
        if (this.name.length() >= 16) {
            return this.name.substring(0, 16);
        } else {
            return this.name;
        }
    }

    public ChatColor getColor() {
        return this.color;
    }

    public void addUUID(UUID uuid) {
        this.players.add(uuid);
    }

    public Color toColor() {
        switch (getColor()) {
            case WHITE:
                return Color.WHITE;
            case GRAY:
                return Color.SILVER;
            case DARK_GRAY:
                return Color.GRAY;
            case BLACK:
                return Color.BLACK;
            case RED:
                return Color.RED;
            case YELLOW:
                return Color.YELLOW;
            case GREEN:
                return Color.LIME;
            case DARK_GREEN:
                return Color.GREEN;
            case AQUA:
                return Color.AQUA;
            case DARK_AQUA:
                return Color.TEAL;
            case DARK_BLUE:
                return Color.NAVY;
            case BLUE:
                return Color.BLUE;
            case DARK_PURPLE:
                return Color.MAROON;
            case DARK_RED:
                return Color.RED;
            case GOLD:
                return Color.ORANGE;
            case LIGHT_PURPLE:
                return Color.FUCHSIA;
            default:
                return Color.YELLOW;
        }
    }
}
