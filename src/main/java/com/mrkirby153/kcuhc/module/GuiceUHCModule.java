package com.mrkirby153.kcuhc.module;

import com.google.inject.AbstractModule;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.UHCGame;

public class GuiceUHCModule extends AbstractModule {

    private final UHC uhc;
    private final UHCGame game;

    public GuiceUHCModule(UHC uhc){
        this.uhc = uhc;
        this.game = uhc.getGame();
    }
    @Override
    protected void configure() {
        bind(UHC.class).toInstance(uhc);
        bind(UHCGame.class).toInstance(game);
    }
}
