package com.mrkirby153.kcuhc.game;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Subcommand;
import com.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.C;
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
}
