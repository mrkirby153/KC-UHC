package com.mrkirby153.kcuhc.module.msc;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.discord.DiscordRobot;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;

public class DiscordModule extends UHCModule {

    private UHC uhc;

    private UHCGame game;

    private DiscordRobot robot;

    @Inject
    public DiscordModule(UHC uhc, UHCGame game) {
        super("Discord", "Enabled discord integration", Material.JUKEBOX);
        this.uhc = uhc;
        this.game = game;
    }

    public DiscordRobot getRobot() {
        return robot;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE) {
            this.game.getTeams().values().forEach(robot::createTeam);
        }
        if (event.getTo() == GameState.ENDING) {
            this.robot.moveAllUsersToLobby();
        }
        if (event.getTo() == GameState.ENDED) {
            this.robot.destroyAllTeams();
        }
    }

    @Override
    public void onLoad() {
        String guild = uhc.getConfig().getString("discord.guild");
        String token = uhc.getConfig().getString("discord.token");

        Bukkit.getServer().getScheduler().runTaskAsynchronously(uhc, () -> {
            robot = new DiscordRobot(token, guild);
            UHC.injector.injectMembers(robot);
            robot.connect();
        });

    }

    @Override
    public void onUnload() {
        robot.disconnect();
    }
}
