package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPGraceModule extends UHCModule {

    private long graceUntil = 0;

    private int GRACE_MINUTES = 5;

    private UHCGame game;

    @Inject
    public PvPGraceModule(UHCGame game) {
        super("PvP Grace", "Prevents PvP damage for a configurable amount of time", Material.DIAMOND_SWORD);
        this.game = game;
        this.autoLoad = true;
    }

    public int getGraceTime() {
        return GRACE_MINUTES;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE) {
            graceUntil = System.currentTimeMillis() + (1000 * 60 * GRACE_MINUTES);
            broadcast(C.m("PvP", "PVP is disabled for {time}",
                    "{time}", Time.format(1, graceUntil - System.currentTimeMillis(), Time.TimeUnit.FIT)).toLegacyText());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.SECOND) {
            if (pvpDisabled())
                announcePvPGrace();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if(event.getDamager().getType() == EntityType.PLAYER){
            if(event.getEntity().getType() == EntityType.PLAYER){
                event.setCancelled(pvpDisabled());
            }
        }
    }

    public void setGraceMinutes(int grace) {
        if (game.getCurrentState() == GameState.ALIVE)
            return;
        this.GRACE_MINUTES = grace;
    }

    private void announcePvPGrace() {
        double msRemaining = this.graceUntil - System.currentTimeMillis();
        double secondsRemaining = Math.floor(msRemaining / 1000D);
        double minuteusRemaining = Math.floor(secondsRemaining / 60D);
        if (secondsRemaining <= 0) {
            broadcast(C.m("PvP", "PVP has been enabled!").toLegacyText(), Sound.ENTITY_ENDERDRAGON_GROWL);
            return;
        }
        if (minuteusRemaining >= 1) {
            if (secondsRemaining % 60 == 0) {
                broadcast(C.m("PvP", "PVP enabled in {time}", "{time}", Time.format(1, (long) msRemaining, Time.TimeUnit.FIT)).toLegacyText(),
                        Sound.BLOCK_NOTE_HAT);
            }
        } else {
            if (secondsRemaining < 10 || (secondsRemaining % 15) == 0) {
                broadcast(C.m("PvP", "PvP enabled in {seconds} seconds", "{seconds}", (int)secondsRemaining).toLegacyText(),
                        Sound.BLOCK_NOTE_HAT);
            }
        }
    }

    private void broadcast(String message) {
        broadcast(message, null);
    }

    private void broadcast(String message, Sound sound) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(message);
            if (sound != null)
                p.playSound(p.getLocation(), sound, SoundCategory.MASTER, 1F, 1F);
        });
    }

    private boolean pvpDisabled() {
        return System.currentTimeMillis() <= graceUntil;
    }
}
