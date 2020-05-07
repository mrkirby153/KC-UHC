package com.mrkirby153.kcuhc.module.health;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;

public class NaturalRegenModule extends UHCModule {

    private UHC uhc;

    private UHCGame game;

    @Inject
    public NaturalRegenModule(UHC uhc, UHCGame game) {
        super("Disable Natural Regeneration", "Disables natural regeneration", Material.POTION);
        this.uhc = uhc;
        this.game = game;
        this.autoLoad = true;
    }

    @EventHandler
    public void updateEvent(UpdateEvent event) {
        if (event.getType() == UpdateType.TWO_SECOND) {
            Boolean rule = game.getUHCWorld().getGameRuleValue(GameRule.NATURAL_REGENERATION);
            if (rule == null || rule) {
                uhc.getLogger()
                    .info("Disabling natural regeneration on " + game.getUHCWorld().getName());
                game.setGameRule(GameRule.NATURAL_REGENERATION, false);
            }
        }
    }

    @Override
    public void onUnload() {
        uhc.getLogger().info("Enabling natural regeneration on " + game.getUHCWorld().getName());
        game.setGameRule(GameRule.NATURAL_REGENERATION, true);
    }
}
