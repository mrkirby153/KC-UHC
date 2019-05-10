package com.mrkirby153.kcuhc;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MinecraftMessageKeys;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mrkirby153.kcuhc.game.GameCommand;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.spectator.CommandSpectate;
import com.mrkirby153.kcuhc.game.spectator.SpectatorHandler;
import com.mrkirby153.kcuhc.game.team.CommandTeam;
import com.mrkirby153.kcuhc.game.team.UHCTeam;
import com.mrkirby153.kcuhc.module.CommandModule;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.player.TeamInventoryModule;
import com.mrkirby153.kcuhc.player.UHCPlayer;
import com.mrkirby153.kcuhc.scoreboard.ScoreboardUpdater;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.command.CommandManager;
import me.mrkirby153.kcutils.cooldown.CooldownManager;
import me.mrkirby153.kcutils.event.UpdateEventHandler;
import me.mrkirby153.kcutils.flags.FlagModule;
import me.mrkirby153.kcutils.flags.WorldFlags;
import me.mrkirby153.kcutils.protocollib.ProtocolLib;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class UHC extends JavaPlugin {

    public static Injector injector;
    private static BukkitCommandManager manager;
    public FlagModule flagModule;
    public ProtocolLib protocolLibManager;
    public SpectatorHandler spectatorHandler;
    public CooldownManager cooldownManager;
    private UpdateEventHandler tickEventHandler;
    private ScoreboardUpdater scoreboardUpdater;
    private UHCGame game;

    /**
     * Gets the ACF command manager
     *
     * @return The manager
     */
    public static BukkitCommandManager getCommandManager() {
        return manager;
    }

    /**
     * Gets the {@link UHCGame}
     *
     * @return The game
     */
    public UHCGame getGame() {
        return game;
    }

    @Override
    public void onDisable() {
        ModuleRegistry.INSTANCE.shutdown();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ModuleRegistry.setPresetDirectory(new File(getDataFolder(), "presets"));

        // Initialize the command manager
        CommandManager.Companion.initialize(this);
        // Initialize ACF
        manager = new BukkitCommandManager(this);

        // Initialize tick event
        tickEventHandler = new UpdateEventHandler(this);
        tickEventHandler.load();

        // Initialize the world flags
        flagModule = new FlagModule(this);
        flagModule.load();
        World world = getServer().getWorlds().get(0);
        flagModule.initialize(world);

        Arrays.stream(WorldFlags.values()).forEach(f -> {
            flagModule.set(world, f, false, false);
        });

        // Initialize protocol lib
        protocolLibManager = new ProtocolLib(this);
        protocolLibManager.load();

        if (protocolLibManager.isErrored()) {
            getLogger().severe("Could not initialize ProtocolLib. Aborting");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.cooldownManager = new CooldownManager(this);
        this.cooldownManager.load();

        // Initialize the game
        game = new UHCGame(this);

        // Initialize Guice injector
        injector = Guice.createInjector(new GuiceModule(this));

        scoreboardUpdater = injector.getInstance(ScoreboardUpdater.class);

        spectatorHandler = injector.getInstance(SpectatorHandler.class);

        registerCommands();
        ModuleRegistry.INSTANCE.loadAll(this);

    }

    private void registerCommands() {
        // Register completions
        manager.getCommandCompletions().registerCompletion("state", c -> {
            List<String> states = new ArrayList<>();
            Arrays.stream(GameState.values()).map(GameState::name).forEach(states::add);
            return states;
        });
        manager.getCommandCompletions()
            .registerCompletion("teams", c -> this.game.getTeams().keySet());
        manager.getCommandCompletions().registerCompletion("modules",
            c -> ModuleRegistry.INSTANCE.availableModules().stream().map(UHCModule::getInternalName)
                .collect(Collectors.toList()));
        manager.getCommandCompletions().registerCompletion("unloadedModules", c -> {
            HashSet<UHCModule> modules = ModuleRegistry.INSTANCE.availableModules();
            modules.removeIf(m ->
                ModuleRegistry.INSTANCE.getLoadedModules().stream().map(UHCModule::getInternalName)
                    .collect(Collectors.toList()).contains(m.getInternalName()));
            return modules.stream().map(UHCModule::getInternalName).collect(Collectors.toList());
        });
        manager.getCommandCompletions().registerCompletion("loadedModules",
            c -> ModuleRegistry.INSTANCE.getLoadedModules().stream().map(UHCModule::getInternalName)
                .collect(Collectors.toList()));
        manager.getCommandCompletions()
            .registerCompletion("presets", c -> ModuleRegistry.INSTANCE.getAvailablePresets());

        manager.getCommandCompletions().registerCompletion("moduleSettings", c -> {
           UHCModule mod =  c.getContextValue(UHCModule.class);
           if(mod == null){
               return new ArrayList<>();
           } else {
               return mod.getSettings().keySet();
           }
        });
        // Register resolvers
        manager.getCommandContexts()
            .registerContext(GameState.class, c -> GameState.valueOf(c.popFirstArg()));
        manager.getCommandContexts().registerContext(UHCPlayer.class, c -> {
            String playerName = c.popFirstArg();
            Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                c.getIssuer()
                    .sendError(MinecraftMessageKeys.NO_PLAYER_FOUND_SERVER, "{search}", playerName);
                throw new InvalidCommandArgument(false);
            }
            return UHCPlayer.getPlayer(player);
        });
        manager.getCommandContexts().registerContext(UHCTeam.class, c -> {
            String name = c.popFirstArg();
            UHCTeam team = this.game.getTeam(name);
            if (team == null) {
                c.getSender().sendMessage(
                    Chat
                        .message("Error", "There is no team by the name of {team}", "{team}", name)
                        .toLegacyText());
                throw new InvalidCommandArgument(false);
            }
            return team;
        });
        manager.getCommandContexts().registerContext(UHCModule.class, c -> {
            String internalName = c.popFirstArg();
            UHCModule mod = ModuleRegistry.INSTANCE.getModuleByName(internalName);
            if (mod == null) {
                c.getSender().sendMessage(
                    Chat.message("Error", "There is no module by the name of {module}",
                        "{module}", internalName
                    ).toLegacyText());
                throw new InvalidCommandArgument(false);
            }
            return mod;
        });

        manager.registerCommand(injector.getInstance(GameCommand.class));
        manager.registerCommand(injector.getInstance(CommandTeam.class));
        manager.registerCommand(injector.getInstance(CommandModule.class));
        manager
            .registerCommand(injector.getInstance(TeamInventoryModule.TeamInventoryCommand.class));
        manager.registerCommand(injector.getInstance(CommandSpectate.class));
    }
}
