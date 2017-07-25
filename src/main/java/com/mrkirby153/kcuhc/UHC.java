package com.mrkirby153.kcuhc;

import co.aikar.commands.BukkitCommandManager;
import com.mrkirby153.kcuhc.game.UHCGame;
import me.mrkirby153.kcutils.command.CommandManager;
import me.mrkirby153.kcutils.event.UpdateEventHandler;
import me.mrkirby153.kcutils.flags.FlagModule;
import org.bukkit.plugin.java.JavaPlugin;

public class UHC extends JavaPlugin {

    private static BukkitCommandManager manager;

    private UpdateEventHandler tickEventHandler;
    private FlagModule flagModule;

    private UHCGame game;

    /**
     * Gets the {@link UHCGame}
     *
     * @return The game
     */
    public UHCGame getGame() {
        return game;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize the command manager
        CommandManager.initialize(this);
        // Initialize ACF
        manager = new BukkitCommandManager(this);

        // Initialize tick event
        tickEventHandler = new UpdateEventHandler(this);
        tickEventHandler.load();

        // Initialize the world flags
        flagModule = new FlagModule(this);
        flagModule.load();
        flagModule.initialize(getServer().getWorlds().get(0));

        // Initialize the game
        game = new UHCGame();
    }
}
