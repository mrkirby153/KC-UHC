package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.ScheduledEvent;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.settings.TimeSetting;
import me.mrkirby153.kcutils.Time;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.EventHandler;

import java.util.concurrent.TimeUnit;

public class FullHealModule extends UHCModule {

    private final UHC plugin;
    private TimeSetting healTime = new TimeSetting("45m");

    private long healTaskId = -1;

    @Inject
    public FullHealModule(UHC plugin) {
        super("Full Heal", "Fully heal players after some time", Material.GLISTERING_MELON_SLICE);
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE) {
            plugin.getLogger().info("[FULL HEAL] Healing in " + (Time
                .format(1, healTime.getValue())));
            healTaskId = plugin.eventTracker.scheduleSyncEvent(new FullHealTask(),
                healTime.getValue(),
                TimeUnit.MILLISECONDS);
        } else {
            if(healTaskId != -1) {
                plugin.eventTracker.cancel(healTaskId);
                healTaskId = -1;
            }
        }
    }

    private class FullHealTask implements ScheduledEvent {

        @Override
        public String getName() {
            return "Final Heal";
        }

        @Override
        public void run() {
            Bukkit.getOnlinePlayers().forEach(player -> {
                AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                player.setHealth(attr != null ? attr.getValue() : 20.0);
                player.setFoodLevel(20);
                player.setSaturation(20.0F);
            });
        }

        @Override
        public boolean shouldAnnounce(long msLeft) {
            double secondsRemaining = Math.floor(msLeft / 1000D);
            double minutesRemaining = Math.floor(secondsRemaining / 60D);
            if (minutesRemaining > 0) {
                return minutesRemaining % 15 == 0;
            } else {
                return secondsRemaining < 10;
            }
        }
    }
}
