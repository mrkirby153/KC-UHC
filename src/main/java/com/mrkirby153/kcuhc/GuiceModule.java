package com.mrkirby153.kcuhc;

import com.google.inject.AbstractModule;
import com.mrkirby153.kcuhc.game.UHCGame;

public class GuiceModule extends AbstractModule {

    private final UHC uhc;
    private final UHCGame game;

    public GuiceModule(UHC uhc) {
        this.uhc = uhc;
        this.game = uhc.getGame();
    }


    @Override
    protected void configure() {
        bind(UHC.class).toInstance(uhc);
        bind(UHCGame.class).toInstance(game);
    }
}
