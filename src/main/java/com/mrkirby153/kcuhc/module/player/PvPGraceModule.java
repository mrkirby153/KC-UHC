package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import java.util.HashMap;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
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
    private UHC uhc;

    @Inject
    public PvPGraceModule(UHC uhc, UHCGame game) {
        super("PvP Grace", "Prevents PvP damage for a configurable amount of time",
            Material.DIAMOND_SWORD);
        this.game = game;
        this.autoLoad = true;
        this.uhc = uhc;
    }

    public int getGraceTime() {
        return GRACE_MINUTES;
    }

    public long getGraceTimeRemaining() {
        return this.graceUntil - System.currentTimeMillis();
    }

    @Override
    public void loadData(HashMap<String, String> data) {
        GRACE_MINUTES = Integer.parseInt(data.get("pvp-grace-time"));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.PLAYER) {
            if (event.getEntity().getType() == EntityType.PLAYER) {
                event.setCancelled(pvpDisabled());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE) {
            graceUntil = System.currentTimeMillis() + (1000 * 60 * GRACE_MINUTES);
            broadcast(Chat.INSTANCE.message("PvP", "PVP is disabled for {time}",
                "{time}",
                Time.INSTANCE.format(1, graceUntil - System.currentTimeMillis(), Time.TimeUnit.FIT))
                .toLegacyText());
        }
        if (event.getTo() == GameState.ENDING) {
            graceUntil = 0;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.SECOND && game.getCurrentState() == GameState.ALIVE) {
            if (pvpDisabled()) {
                announcePvPGrace();
            }
        }
    }

    @Override
    public void saveData(HashMap<String, String> data) {
        data.put("pvp-grace-time", Integer.toString(GRACE_MINUTES));
    }

    public void setGraceMinutes(int grace) {
        if (game.getCurrentState() == GameState.ALIVE) {
            return;
        }
        this.GRACE_MINUTES = grace;
    }

    private void announcePvPGrace() {
        double msRemaining = this.graceUntil - System.currentTimeMillis();
        double secondsRemaining = Math.floor(msRemaining / 1000D);
        double minuteusRemaining = Math.floor(secondsRemaining / 60D);
        if (secondsRemaining <= 0) {
            broadcast(Chat.INSTANCE.message("PvP", "PVP has been enabled!").toLegacyText(),
                Sound.ENTITY_ENDERDRAGON_GROWL);
            return;
        }
        if (minuteusRemaining >= 1) {
            if (secondsRemaining % 60 == 0) {
                BaseComponent component = Chat.INSTANCE
                    .formattedChat("Grace period ends in", ChatColor.RED, Chat.Style.BOLD);
                component.addExtra(Chat.INSTANCE.formattedChat(
                    " " + Time.INSTANCE.format(1, (long) msRemaining, Time.TimeUnit.FIT),
                    ChatColor.AQUA, Chat.Style.BOLD));
                component
                    .addExtra(Chat.INSTANCE.formattedChat("!", ChatColor.RED, Chat.Style.BOLD));
                Bukkit.getOnlinePlayers().forEach(p -> {
                    this.uhc.protocolLibManager.sendActionBar(p, component);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                });
            }
        } else {
            if (secondsRemaining < 10 || (secondsRemaining % 15) == 0) {
                BaseComponent component = Chat.INSTANCE
                    .formattedChat("Grace period ends in", ChatColor.RED, Chat.Style.BOLD);
                component.addExtra(Chat.INSTANCE
                    .formattedChat(" " + (int) secondsRemaining, ChatColor.AQUA, Chat.Style.BOLD));
                component.addExtra(
                    Chat.INSTANCE.formattedChat(" seconds!", ChatColor.RED, Chat.Style.BOLD));
                Bukkit.getOnlinePlayers().forEach(p -> {
                    this.uhc.protocolLibManager.sendActionBar(p, component);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                });
            }
        }
    }

    private void broadcast(String message) {
        broadcast(message, null);
    }

    private void broadcast(String message, Sound sound) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(message);
            if (sound != null) {
                p.playSound(p.getLocation(), sound, SoundCategory.MASTER, 1F, 1F);
            }
        });
    }

    private boolean pvpDisabled() {
        return System.currentTimeMillis() <= graceUntil;
    }
}
