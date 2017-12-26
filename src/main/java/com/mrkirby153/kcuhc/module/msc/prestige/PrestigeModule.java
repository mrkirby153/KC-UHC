package com.mrkirby153.kcuhc.module.msc.prestige;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class PrestigeModule extends UHCModule {

    private final File prestigeFile;
    private HashMap<UUID, Integer> prestigeMap = new HashMap<>();

    private HashMap<UUID, PrestigeTeam> teams = new HashMap<>();

    private UHC uhc;

    @Inject
    public PrestigeModule(UHC uhc) {
        super("Prestige", "Display the user's win count", Material.CAKE);
        autoLoad = true;
        this.uhc = uhc;
        this.prestigeFile = new File(uhc.getDataFolder(), "prestige.yml");
    }

    @Override
    public void onLoad() {
        this.loadPrestige();
    }

    @Override
    public void onUnload() {

    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.FAST) {
            Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId)
                .filter(u -> !teams.containsKey(u)).forEach(p -> {
                teams.put(p, new PrestigeTeam(this.uhc, p));
            });
            // Remove offline players
            teams.entrySet().removeIf(
                entry -> !Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).collect(
                    Collectors.toList()).contains(entry.getKey()));
            teams.values().forEach(PrestigeTeam::update);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        event.setFormat(
            "[" + getPrestigeMap().getOrDefault(event.getPlayer().getUniqueId(), 0) + ChatColor.GOLD
                + " ⭐" + ChatColor.WHITE + "] " + event.getFormat());
    }

    private void loadPrestige() {
        YamlConfiguration cfg = new YamlConfiguration();
        if (!prestigeFile.exists()) {
            return;
        }
        try {
            cfg.load(this.prestigeFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            return;
        }
        for (String s : cfg.getKeys(false)) {
            prestigeMap.put(UUID.fromString(s), cfg.getInt(s));
            uhc.getLogger().info("[PRESTIGE] Loading " + s + " (" + cfg.getInt(s) + ")");
        }
        prestigeMap.keySet().forEach(u -> teams.put(u, new PrestigeTeam(this.uhc, u)));
    }

    public HashMap<UUID, Integer> getPrestigeMap() {
        return prestigeMap;
    }

    public HashMap<UUID, PrestigeTeam> getTeams() {
        return teams;
    }
}
