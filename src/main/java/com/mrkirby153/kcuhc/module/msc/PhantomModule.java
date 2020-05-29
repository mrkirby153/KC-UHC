package com.mrkirby153.kcuhc.module.msc;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.GameRule;
import org.bukkit.Material;

public class PhantomModule extends UHCModule {

    private UHCGame game;

    @Inject
    public PhantomModule(UHCGame uhcGame) {
        super("Disable Phantoms", "Disables the spawning of phantoms", Material.PHANTOM_SPAWN_EGG);
        this.game = uhcGame;
    }

    @Override
    public void onLoad() {
        game.setGameRule(GameRule.DO_INSOMNIA, false);
    }

    @Override
    public void onUnload() {
        game.setGameRule(GameRule.DO_INSOMNIA, true);
    }
}
