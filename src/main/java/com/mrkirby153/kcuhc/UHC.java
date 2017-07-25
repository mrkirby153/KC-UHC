package com.mrkirby153.kcuhc;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MinecraftMessageKeys;
import com.mrkirby153.kcuhc.game.GameCommand;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.player.UHCPlayer;
import me.mrkirby153.kcutils.command.CommandManager;
import me.mrkirby153.kcutils.event.UpdateEventHandler;
import me.mrkirby153.kcutils.flags.FlagModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        game = new UHCGame(this);

        registerCommands();
    }

    private void registerCommands(){
        // Register completions
        manager.getCommandCompletions().registerCompletion("state", c-> {
            List<String> states = new ArrayList<>();
            Arrays.stream(GameState.values()).map(GameState::name).forEach(states::add);
            return states;
        });

        // Register resolvers
        manager.getCommandContexts().registerContext(GameState.class, c -> GameState.valueOf(c.popFirstArg()));
        manager.getCommandContexts().registerContext(UHCPlayer.class, c -> {
            String playerName = c.popFirstArg();
            Player player = Bukkit.getPlayer(playerName);
            if(player == null){
                c.getIssuer().sendError(MinecraftMessageKeys.NO_PLAYER_FOUND_SERVER, "{search}", playerName);
                throw new InvalidCommandArgument(false);
            }
            return UHCPlayer.getPlayer(player);
        });

        manager.registerCommand(new GameCommand(game, this));
    }
}
