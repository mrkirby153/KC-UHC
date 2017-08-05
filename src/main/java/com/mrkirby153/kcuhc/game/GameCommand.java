package com.mrkirby153.kcuhc.game;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import com.google.common.base.Throwables;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.msc.HeightBuildingModule;
import com.mrkirby153.kcuhc.module.msc.cornucopia.CornucopiaModule;
import com.mrkirby153.kcuhc.module.player.PvPGraceModule;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.C;
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

    public GameCommand(UHCGame game, UHC uhc) {
        this.game = game;
        this.uhc = uhc;
    }

    @Subcommand("build-height")
    public void buildHeight(CommandSender sender, @Optional Integer height) {
        java.util.Optional<HeightBuildingModule> wbm = ModuleRegistry.INSTANCE.getLoadedModule(HeightBuildingModule.class);
        if (!wbm.isPresent()) {
            sender.sendMessage(C.e("The height module is not loaded").toLegacyText());
            return;
        }
        if (height == null) {
            sender.sendMessage(C.m("Build Height", "The build height is set to {height} blocks",
                    "{height}", wbm.get().getMaxBuildHeight()).toLegacyText());
        } else {
            wbm.get().setMaxBuildHeight(height);
            sender.sendMessage(C.m("Build Height", "Set build height to {height} blocks", "{height}", height).toLegacyText());
        }
    }

    @Subcommand("grace")
    public void grace(CommandSender sender, @Optional Integer minutes) {
        ModuleRegistry.INSTANCE.getLoadedModule(PvPGraceModule.class).ifPresent(mod -> {
            if (minutes == null) {
                sender.sendMessage(C.m("Grace", "Grace is currently {time} minutes", "{time}", mod.getGraceTime()).toLegacyText());
            } else {
                mod.setGraceMinutes(minutes);
                sender.sendMessage(C.m("Grace", "Set grace to {time} minutes", "{time}", minutes).toLegacyText());
            }
        });
    }

    @Subcommand("pregen|pregenerate")
    public void pregenerateWorld(CommandSender sender) {
        this.game.generate();
        sender.sendMessage(C.m("Game", "Pregeneration started!").toLegacyText());
    }

    @Subcommand("stalemate")
    public void resolveStalemate(Player sender, @Optional String code) {
        java.util.Optional<WorldBorderModule> mod = ModuleRegistry.INSTANCE.getLoadedModule(WorldBorderModule.class);
        if (!mod.isPresent()) {
            sender.sendMessage(C.e("Worldborder module not loaded!").toLegacyText());
            return;
        }
        if (uhc.getGame().getCurrentState() != GameState.ALIVE) {
            sender.sendMessage(C.e("This can only be run when the game is active!").toLegacyText());
            return;
        }
        if (code != null) {
            String requiredCode = this.stalemateCode.get(sender.getUniqueId());
            if (requiredCode != null && requiredCode.equalsIgnoreCase(code)) {
                sender.sendMessage(C.m("Stalemate", "Stalemate resolution system activated!").toLegacyText());
                this.stalemateCode.remove(sender.getUniqueId());
                mod.get().resolveStalemate();
            } else {
                sender.sendMessage(C.m("Stalemate", "Code incorrect.").toLegacyText());
            }
        } else {
            int c = (int) (Math.random() * 10000);
            this.stalemateCode.put(sender.getUniqueId(), Integer.toString(c));
            sender.sendMessage(C.m("Stalemate", "Are you sure you want to activate the stalemate resolution system? " +
                    "Type {command} to confirm", "{command}", "/game stalemate " + c).toLegacyText());
        }
    }

    @Subcommand("state")
    @CommandCompletion("@state")
    public void setGameState(CommandSender sender, GameState state) {
        game.setCurrentState(state);
        sender.sendMessage(C.m("Game", "Set game state to {state}", "{state}", state).toLegacyText());
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
        sender.sendMessage(C.m("Game", "Started").toLegacyText());
        if (sender instanceof Player) {
            Player p = (Player) sender;
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F);
        }
    }

    @Subcommand("stop")
    public void stopGame(CommandSender sender) {
        this.game.stop("Nobody", Color.GREEN);
        sender.sendMessage(C.m("Game", "Stopped!").toLegacyText());
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
                sender.sendMessage(C.e("Worldborder is not loaded!").toLegacyText());
                return;
            }
            WorldBorderModule module = ModuleRegistry.INSTANCE.getModule(WorldBorderModule.class);
            sender.sendMessage(C.m("Worldborder", "Worldborder will move from {start} to {end} in {time}",
                    "{start}", module.getStartSize(),
                    "{end}", module.getEndSize(),
                    "{time}", Time.format(2, module.getDuration() * 1000, Time.TimeUnit.FIT)).toLegacyText());
        }

        @Subcommand("update")
        public void newTime(CommandSender sender, Integer newTime) {
            if (!ModuleRegistry.INSTANCE.loaded(WorldBorderModule.class)) {
                sender.sendMessage(C.e("Worldborder is not loaded!").toLegacyText());
                return;
            }
            if (game.getCurrentState() != GameState.ALIVE) {
                sender.sendMessage(C.e("This command can only be used when the game is alive!").toLegacyText());
                return;
            }
            WorldBorderModule module = ModuleRegistry.INSTANCE.getModule(WorldBorderModule.class);
            module.updateSpeed(newTime);
        }

        @Subcommand("set")
        public void setBorder(CommandSender sender, Integer start, Integer end, Integer time) {
            if (game.getCurrentState() != GameState.WAITING) {
                sender.sendMessage(C.e("You cannot change the worldborder while the game is in motion!").toLegacyText());
            }
            if (!ModuleRegistry.INSTANCE.loaded(WorldBorderModule.class)) {
                sender.sendMessage(C.e("Worldborder is not loaded!").toLegacyText());
                return;
            }
            WorldBorderModule module = ModuleRegistry.INSTANCE.getModule(WorldBorderModule.class);
            module.setStartSize(start);
            module.setEndSize(end);
            module.setDuration(time);
            sender.sendMessage(C.m("Worldborder", "Worldborder will move from {start} to {end} in {time}",
                    "{start}", module.getStartSize(),
                    "{end}", module.getEndSize(),
                    "{time}", Time.format(2, module.getDuration() * 1000, Time.TimeUnit.FIT)).toLegacyText());
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
            sender.sendMessage(C.m("Presets", "Available presets ({count}): {presets}",
                    "{count}", ModuleRegistry.INSTANCE.getAvailablePresets().size(),
                    "{presets}", presets).toLegacyText());
        }

        @Subcommand("save")
        public void savePreset(CommandSender sender, String name) {
            try {
                ModuleRegistry.INSTANCE.saveToPreset(name);
                sender.sendMessage(C.m("Preset", "Saved preset {preset}",
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
                sender.sendMessage(C.m("Preset", "Loaded preset {preset}",
                        "{preset}", preset).toLegacyText());
            } catch (FileNotFoundException e) {
                sender.sendMessage(C.e("That preset doesn't exist").toLegacyText());
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
    }
}
