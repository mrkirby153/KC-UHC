package com.mrkirby153.kcuhc.module.msc;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.discord.DiscordRobot;
import com.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public class DiscordModule  extends UHCModule{

    private UHC uhc;

    private DiscordRobot robot;

    @Inject
    public DiscordModule(UHC uhc){
        super("Discord", "Enabled discord integration", Material.JUKEBOX);
        this.uhc = uhc;
    }

    public DiscordRobot getRobot() {
        return robot;
    }

    @Override
    public void onLoad() {
        String guild = uhc.getConfig().getString("discord.guild");
        String token = uhc.getConfig().getString("discord.token");

        Bukkit.getServer().getScheduler().runTaskAsynchronously(uhc, ()->{
            robot = new DiscordRobot(token, guild);
            robot.connect();
        });

    }

    @Override
    public void onUnload() {
        robot.disconnect();
    }
}
