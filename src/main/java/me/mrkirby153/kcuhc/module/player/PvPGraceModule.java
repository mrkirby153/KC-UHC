package me.mrkirby153.kcuhc.module.player;

import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTime;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPGraceModule extends UHCModule {

    private long graceUntil = -1;

    public PvPGraceModule() {
        super(Material.DIAMOND_SWORD, 0, "PvP Grace", false, "Prevents PvP combat for some time");
    }

    @Override
    public void onEnable() {
        this.graceUntil = -1;
        Bukkit.broadcastMessage(UtilChat.message("PvP Grace Enabled!"));
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("PvP Grace Disabled!"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == UHCArena.State.RUNNING) {
            if (getPlugin().arena.getProperties().PVP_GRACE_MINS.get() <= 0) {
                ModuleRegistry.unloadModule(this);
            }
            this.graceUntil = System.currentTimeMillis() + (1000 * 60) * getPlugin().arena.getProperties().PVP_GRACE_MINS.get();
            Bukkit.broadcastMessage(UtilChat.message(org.bukkit.ChatColor.BOLD + "" + org.bukkit.ChatColor.GOLD + "PVP is disabled for " +
                    UtilTime.format(1, graceUntil - System.currentTimeMillis(), UtilTime.TimeUnit.FIT)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.SECOND)
            if (pvpDisabled())
                announcePvPGrace();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.PLAYER && event.getEntity().getType() == EntityType.PLAYER) {
            if (pvpDisabled()) {
                event.setCancelled(true);
            }
        }
        if (event.getDamager().getType() == EntityType.ARROW && event.getEntity().getType() == EntityType.PLAYER) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.getShooter() instanceof Player) {
                if (pvpDisabled()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public boolean pvpDisabled() {
        return this.graceUntil != -1 && System.currentTimeMillis() < graceUntil;
    }

    private void announcePvPGrace() {
        double msRemaining = this.graceUntil - System.currentTimeMillis();
        double secondsRemaining = Math.floor(msRemaining / 1000D);
        double minutesRemaining = Math.floor(secondsRemaining / 60D);
        if (secondsRemaining <= 0) {
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1F, 1F));
            Bukkit.broadcastMessage(UtilChat.message(ChatColor.RED + "" + ChatColor.BOLD + "PVP ENABLED!"));
            return;
        }
        if (minutesRemaining >= 1) {
            if (secondsRemaining % 60 == 0) {
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F));
                Bukkit.broadcastMessage(UtilChat.message(ChatColor.GREEN + "PVP will be enabled in " + UtilTime.format(1, (long) msRemaining, UtilTime.TimeUnit.FIT)));
            }
        } else {
            if (secondsRemaining < 10 || secondsRemaining == 30 || secondsRemaining == 15) {
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F));
                Bukkit.broadcastMessage(UtilChat.message(ChatColor.GREEN + "" + ChatColor.BOLD + "PVP will be enabled in " + secondsRemaining + " seconds"));
            }
        }
    }
}
