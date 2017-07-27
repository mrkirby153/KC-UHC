package com.mrkirby153.kcuhc.game;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Subcommand;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.worldborder.WorldBorderModule;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.Time;
import org.bukkit.command.CommandSender;

@CommandAlias("game")
public class GameCommand extends BaseCommand {

    private final UHCGame game;
    private final UHC uhc;

    public GameCommand(UHCGame game, UHC uhc) {
        this.game = game;
        this.uhc = uhc;
    }


    @Subcommand("state")
    @CommandCompletion("@state")
    public void setGameState(CommandSender sender, GameState state){
        game.setCurrentState(state);
        sender.sendMessage(C.m("Game", "Set game state to {state}", "{state}", state).toLegacyText());
    }

    @Subcommand("border")
    public class BorderCommands extends BaseCommand {

        @Subcommand("get")
        public void getBorder(CommandSender sender){
            if(!ModuleRegistry.INSTANCE.loaded(WorldBorderModule.class)){
                sender.sendMessage(C.e("Worldborder is not loaded!").toLegacyText());
                return;
            }
            WorldBorderModule module = ModuleRegistry.INSTANCE.getModule(WorldBorderModule.class);
            sender.sendMessage(C.m("Worldborder", "Worldborder will move from {start} to {end} in {time}",
                    "{start}", module.getStartSize(),
                    "{end}", module.getEndSize(),
                    "{time}", Time.format(2, module.getDuration() * 1000, Time.TimeUnit.FIT)).toLegacyText());
        }

        @Subcommand("set")
        public void setBorder(CommandSender sender, Integer start, Integer end, Integer time){
            if(game.getCurrentState() != GameState.WAITING){
                sender.sendMessage(C.e("You cannot change the worldborder while the game is in motion!").toLegacyText());
            }
            if(!ModuleRegistry.INSTANCE.loaded(WorldBorderModule.class)){
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

        @Subcommand("update")
        public void newTime(CommandSender sender, Integer newTime){
            if(!ModuleRegistry.INSTANCE.loaded(WorldBorderModule.class)){
                sender.sendMessage(C.e("Worldborder is not loaded!").toLegacyText());
                return;
            }
            if(game.getCurrentState() != GameState.ALIVE){
                sender.sendMessage(C.e("This command can only be used when the game is alive!").toLegacyText());
                return;
            }
            WorldBorderModule module = ModuleRegistry.INSTANCE.getModule(WorldBorderModule.class);
            module.updateSpeed(newTime);
        }
    }
}
