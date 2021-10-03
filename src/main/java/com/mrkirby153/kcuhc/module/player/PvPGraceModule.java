package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.EventTracker;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.ScheduledEvent;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.settings.TimeSetting;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.TimeUnit;

public class PvPGraceModule extends UHCModule {

    private final EventTracker eventTracker;
    private boolean pvpDisabled = false;
    private TimeSetting graceTime = new TimeSetting("5m");
    private long taskId = -1;

    @Inject
    public PvPGraceModule(UHC uhc) {
        super("PvP Grace", "Prevents PvP damage for a configurable amount of time",
            Material.DIAMOND_SWORD);
        this.autoLoad = true;
        this.eventTracker = uhc.eventTracker;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.PLAYER) {
            if (event.getEntity().getType() == EntityType.PLAYER) {
                event.setCancelled(this.pvpDisabled);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE) {
            this.pvpDisabled = true;
            this.taskId = eventTracker.scheduleSyncEvent(new NoMoreGraceEvent(),
                graceTime.getValue(), TimeUnit.MILLISECONDS);
        }
        if (event.getTo() == GameState.ENDING) {
            this.pvpDisabled = false;
            if (this.taskId != -1) {
                eventTracker.cancel(this.taskId);
                this.taskId = -1;
            }
        }
    }

    private class NoMoreGraceEvent implements ScheduledEvent {

        @Override
        public String getName() {
            return "PvP Enabled";
        }

        @Override
        public void run() {
            pvpDisabled = false;
        }

        @Override
        public void onCancel() {
            pvpDisabled = false;
        }

        @Override
        public boolean shouldAnnounce(long msLeft) {
            double secondsLeft = Math.floor(msLeft / 1000D);
            double minutesLeft = Math.floor(secondsLeft / 60D);
            if (minutesLeft > 2) {
                return secondsLeft % 60 == 0;
            } else {
                return secondsLeft < 10 || (secondsLeft % 30 == 0);
            }
        }

        @Override
        public Sound startSound() {
            return Sound.ENTITY_ENDER_DRAGON_GROWL;
        }
    }
}
