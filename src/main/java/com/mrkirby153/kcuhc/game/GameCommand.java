package com.mrkirby153.kcuhc.game;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.msc.cornucopia.CornucopiaModule;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Time;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

@CommandAlias("game")
public class GameCommand extends BaseCommand {

    private final UHCGame game;
    private final UHC uhc;

    private HashMap<UUID, String> stalemateCode = new HashMap<>();

    @Inject
    public GameCommand(UHCGame game, UHC uhc) {
        this.game = game;
        this.uhc = uhc;
    }

    @Subcommand("pregen|pregenerate")
    public void pregenerateWorld(CommandSender sender) {
        this.game.generate();
        sender.sendMessage(Chat.INSTANCE.message("Game", "Pregeneration started!").toLegacyText());
    }

    @Subcommand("stalemate")
    public void resolveStalemate(Player sender, @Optional String code) {
        java.util.Optional<WorldBorderModule> mod = ModuleRegistry.INSTANCE
            .getLoadedModule(WorldBorderModule.class);
        if (!mod.isPresent()) {
            sender
                .sendMessage(Chat.INSTANCE.error("Worldborder module not loaded!").toLegacyText());
            return;
        }
        if (uhc.getGame().getCurrentState() != GameState.ALIVE) {
            sender.sendMessage(Chat.INSTANCE.error("This can only be run when the game is active!")
                .toLegacyText());
            return;
        }
        if (code != null) {
            String requiredCode = this.stalemateCode.get(sender.getUniqueId());
            if (requiredCode != null && requiredCode.equalsIgnoreCase(code)) {
                sender.sendMessage(
                    Chat.INSTANCE.message("Stalemate", "Stalemate resolution system activated!")
                        .toLegacyText());
                this.stalemateCode.remove(sender.getUniqueId());
                mod.get().resolveStalemate();
            } else {
                sender.sendMessage(
                    Chat.INSTANCE.message("Stalemate", "Code incorrect.").toLegacyText());
            }
        } else {
            int c = (int) (Math.random() * 10000);
            this.stalemateCode.put(sender.getUniqueId(), Integer.toString(c));
            sender.sendMessage(Chat.INSTANCE.message("Stalemate",
                "Are you sure you want to activate the stalemate resolution system? " +
                    "Type {command} to confirm", "{command}", "/game stalemate " + c)
                .toLegacyText());
        }
    }

    @Subcommand("state")
    @CommandCompletion("@state")
    public void setGameState(CommandSender sender, GameState state) {
        game.setCurrentState(state);
        sender.sendMessage(
            Chat.INSTANCE.message("Game", "Set game state to {state}", "{state}", state)
                .toLegacyText());
    }

    @Subcommand("cornucopia")
    public void spawnCorn(CommandSender sender, @Default("100") Integer size) {
        ModuleRegistry.INSTANCE.getLoadedModule(CornucopiaModule.class).ifPresent(mod -> {
            mod.spawnCornucopia(size);
        });
    }

    @Subcommand("start")
    public void startGame(CommandSender sender) {
        this.game.setCurrentState(GameState.COUNTDOWN);
        sender.sendMessage(Chat.INSTANCE.message("Game", "Started").toLegacyText());
        if (sender instanceof Player) {
            Player p = (Player) sender;
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F);
        }
    }

    @Subcommand("stop")
    public void stopGame(CommandSender sender) {
        this.game.stop("Nobody", Color.GREEN);
        sender.sendMessage(Chat.INSTANCE.message("Game", "Stopped!").toLegacyText());
        if (sender instanceof Player) {
            Player p = (Player) sender;
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F);
        }
    }

    @Subcommand("border")
    public class BorderCommands extends BaseCommand {

        @Subcommand("get")
        public void getBorder(CommandSender sender) {
            if (!ModuleRegistry.INSTANCE.loaded(WorldBorderModule.class)) {
                sender
                    .sendMessage(Chat.INSTANCE.error("Worldborder is not loaded!").toLegacyText());
                return;
            }
            WorldBorderModule module = ModuleRegistry.INSTANCE.getModule(WorldBorderModule.class);
            sender.sendMessage(Chat.INSTANCE
                .message("Worldborder", "Worldborder will move from {start} to {end} in {time}",
                    "{start}", module.getStartSize(),
                    "{end}", module.getEndSize(),
                    "{time}",
                    Time.INSTANCE.format(2, module.getDuration() * 1000, Time.TimeUnit.FIT))
                .toLegacyText());
        }

        @Subcommand("update")
        public void newTime(CommandSender sender, String newTime) {
            if (!ModuleRegistry.INSTANCE.loaded(WorldBorderModule.class)) {
                sender
                    .sendMessage(Chat.INSTANCE.error("Worldborder is not loaded!").toLegacyText());
                return;
            }
            if (game.getCurrentState() != GameState.ALIVE) {
                sender.sendMessage(
                    Chat.INSTANCE.error("This command can only be used when the game is alive!")
                        .toLegacyText());
                return;
            }
            WorldBorderModule module = ModuleRegistry.INSTANCE.getModule(WorldBorderModule.class);
            long time = Time.INSTANCE.parse(newTime) / 1000;
            if(time < 1){
                sender.sendMessage(Chat.INSTANCE.error("Time must be greater than 1 second!").toLegacyText());
                return;
            }
            module.updateSpeed((int) time);
        }
    }


    @Subcommand("preset")
    public class PresetCommands extends BaseCommand {

        @Default
        public void listPresets(CommandSender sender) {
            StringBuilder builder = new StringBuilder();
            ModuleRegistry.INSTANCE.getAvailablePresets().forEach(p -> {
                builder.append(p).append(", ");
            });
            String presets = builder.toString().trim();
            sender.sendMessage(
                Chat.INSTANCE.message("Presets", "Available presets ({count}): {presets}",
                    "{count}", ModuleRegistry.INSTANCE.getAvailablePresets().size(),
                    "{presets}", presets).toLegacyText());
        }

        @Subcommand("save")
        public void savePreset(CommandSender sender, String name) {
            try {
                ModuleRegistry.INSTANCE.saveToPreset(name);
                sender.sendMessage(Chat.INSTANCE.message("Preset", "Saved preset {preset}",
                    "{preset}", name).toLegacyText());
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }

        @Subcommand("load")
        @CommandCompletion("@presets")
        public void setPreset(CommandSender sender, String preset) {
            try {
                ModuleRegistry.INSTANCE.loadFromPreset(preset);
                sender.sendMessage(Chat.INSTANCE.message("Preset", "Loaded preset {preset}",
                    "{preset}", preset).toLegacyText());
            } catch (FileNotFoundException e) {
                sender.sendMessage(Chat.INSTANCE.error("That preset doesn't exist").toLegacyText());
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
    }
}
