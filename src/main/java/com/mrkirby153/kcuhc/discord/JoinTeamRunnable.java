package com.mrkirby153.kcuhc.discord;

import com.mrkirby153.kcuhc.discord.objects.UHCTeamObject;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class JoinTeamRunnable implements Runnable {

    private int taskId;

    private UHCTeam team;
    private UHCTeamObject object;
    private JavaPlugin plugin;
    private DiscordModule module;

    protected JoinTeamRunnable(JavaPlugin plugin, UHCTeam team, UHCTeamObject object,
        DiscordModule module) {
        this.team = team;
        this.object = object;
        this.plugin = plugin;
        this.taskId = this.plugin.getServer().getScheduler()
            .scheduleSyncRepeatingTask(plugin, this, 0L, 5L);
        this.module = module;
    }

    @Override
    public void run() {
        if (!this.object.isCreated()) {
            return;
        }
        plugin.getLogger().info("[DISCORD] Team " + this.team.getTeamName()
            + " has been created, assigning players...");
        this.team.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
            .filter(p -> this.module.getMapper().getUser(p.getUniqueId()) != null).forEach(
            p -> this.object.joinTeam(this.module.getMapper().getUser(p.getUniqueId()),
                this.object::moveToTeamChannel));
        this.plugin.getServer().getScheduler().cancelTask(this.taskId);
    }
}
