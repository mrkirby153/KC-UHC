package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.UHCGame;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.settings.TimeSetting;
import com.mrkirby153.kcuhc.player.ActionBar;
import com.mrkirby153.kcuhc.player.ActionBarManager;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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

    private TimeSetting graceTime = new TimeSetting("5m");

    private UHCGame game;
    private UHC uhc;

    private ActionBar pvpActionBar = new ActionBar("pvp", 1);

    @Inject
    public PvPGraceModule(UHC uhc, UHCGame game) {
        super("PvP Grace", "Prevents PvP damage for a configurable amount of time",
            Material.DIAMOND_SWORD);
        this.game = game;
        this.autoLoad = true;
        this.uhc = uhc;
    }

    public long getGraceTime() {
        return graceTime.getValue();
    }

    public long getGraceTimeRemaining() {
        return this.graceUntil - System.currentTimeMillis();
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
            graceUntil = System.currentTimeMillis() + graceTime.getValue();
            broadcast(Chat.message("PvP", "PVP is disabled for {time}",
                "{time}",
                Time.INSTANCE.format(1, graceUntil - System.currentTimeMillis(), Time.TimeUnit.FIT))
                .toLegacyText());
        }
        if (event.getTo() == GameState.ENDING) {
            graceUntil = 0;
            pvpActionBar.clearAll();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.SECOND && game.getCurrentState() == GameState.ALIVE) {
            if (pvpDisabled()) {
                announcePvPGrace();
            }
        }
        if (event.getType() == UpdateType.FAST && game.getCurrentState() == GameState.ALIVE) {
            if (pvpDisabled()) {
                updatePvpActionBar();
            } else {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (pvpActionBar.get(player) != null) {
                        pvpActionBar.clear(player);
                    }
                });
            }
        }
    }

    @Override
    public void onLoad() {
        ActionBarManager.getInstance().registerActionBar(pvpActionBar);
    }

    @Override
    public void onUnload() {
        ActionBarManager.getInstance().registerActionBar(pvpActionBar);
    }

    private void updatePvpActionBar() {
        TextComponent tc = new TextComponent("PVP » ");
        tc.setColor(ChatColor.RED);
        TextComponent time = new TextComponent(
            shorthandTime(this.graceUntil - System.currentTimeMillis()));
        time.setColor(ChatColor.GRAY);
        tc.addExtra(time);
        Bukkit.getOnlinePlayers().forEach(player -> {
            pvpActionBar.set(player, tc);
        });
    }


    private String shorthandTime(long ms) {
        double seconds = Math.floor(ms / 1000D);
        double minutes = Math.floor(seconds / 60D);

        StringBuilder sb = new StringBuilder();
        sb.append((int) minutes).append("m");
        int s = (int) seconds % 60;
        if (s < 10) {
            sb.append("0");
        }
        sb.append(s).append("s");
        return sb.toString();
    }

    private void announcePvPGrace() {
        double msRemaining = this.graceUntil - System.currentTimeMillis();
        double secondsRemaining = Math.floor(msRemaining / 1000D);
        double minuteusRemaining = Math.floor(secondsRemaining / 60D);
        if (secondsRemaining <= 0) {
            broadcast(Chat.message("PvP", "PVP has been enabled!").toLegacyText(),
                Sound.ENTITY_ENDER_DRAGON_GROWL);
            return;
        }
        if (minuteusRemaining >= 1) {
            if (secondsRemaining % 60 == 0) {
                BaseComponent component = Chat
                    .formattedChat("Grace period ends in", ChatColor.RED, Chat.Style.BOLD);
                component.addExtra(Chat.formattedChat(
                    " " + Time.INSTANCE.format(1, (long) msRemaining, Time.TimeUnit.FIT),
                    ChatColor.AQUA, Chat.Style.BOLD));
                component
                    .addExtra(Chat.formattedChat("!", ChatColor.RED, Chat.Style.BOLD));
                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.spigot().sendMessage(component);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 1F);
                });
            }
        } else {
            if (secondsRemaining < 10 || (secondsRemaining % 15) == 0) {
                BaseComponent component = Chat
                    .formattedChat("Grace period ends in", ChatColor.RED, Chat.Style.BOLD);
                component.addExtra(Chat
                    .formattedChat(" " + (int) secondsRemaining, ChatColor.AQUA, Chat.Style.BOLD));
                component.addExtra(
                    Chat.formattedChat(" seconds!", ChatColor.RED, Chat.Style.BOLD));
                Bukkit.getOnlinePlayers().forEach(p -> {
                   p.spigot().sendMessage(component);
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
