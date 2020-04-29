package com.mrkirby153.kcuhc.player;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionBar {

    private final Map<UUID, BaseComponent> bars = new HashMap<>();
    private String id;
    private int priority;

    public ActionBar(String id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    public void set(Player player, BaseComponent text) {
        synchronized (bars) {
            bars.put(player.getUniqueId(), text);
        }
    }

    public BaseComponent get(Player player) {
        return bars.get(player.getUniqueId());
    }

    public void clear(Player player) {
        synchronized (bars) {
            bars.remove(player.getUniqueId());
        }
    }

    public void clearAll() {
        synchronized (bars) {
            bars.clear();
        }
    }

    public String getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }
}
