package com.mrkirby153.kcuhc;

import co.aikar.commands.BukkitCommandManager;
import me.mrkirby153.kcutils.command.CommandManager;
import org.bukkit.plugin.java.JavaPlugin;

public class UHC extends JavaPlugin {

    private static BukkitCommandManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize the command manager
        CommandManager.initialize(this);
        // Initialize ACF
        manager = new BukkitCommandManager(this);
    }
}
