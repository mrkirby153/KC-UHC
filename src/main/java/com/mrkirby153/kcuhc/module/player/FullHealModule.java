package com.mrkirby153.kcuhc.module.player;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.settings.TimeSetting;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;

public class FullHealModule extends UHCModule {

    private final UHC plugin;
    private TimeSetting healTime = new TimeSetting("45m");
    private long healAt = -1L;
    private boolean healed = false;

    @Inject
    public FullHealModule(UHC plugin) {
        super("Full Heal", "Fully heal players after some time", Material.GLISTERING_MELON_SLICE);
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE) {
            this.healAt = System.currentTimeMillis() + healTime.getValue();
            this.healed = false;
            plugin.getLogger().info("[FULL HEAL] Healing in " + (Time.INSTANCE
                .format(1, this.healAt - System.currentTimeMillis())));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.SECOND
            && this.plugin.getGame().getCurrentState() == GameState.ALIVE) {
            if (!healed) {
                announceFullHeal();
                if(this.healAt < System.currentTimeMillis()) {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                        player.setFoodLevel(20);
                        player.setSaturation(20.0F);
                    });
                    healed = true;
                }
            }
        }
    }

    private void announceFullHeal() {
        double msRemaining = this.healAt - System.currentTimeMillis();
        double secondsRemaining = Math.floor(msRemaining / 1000D);
        double minutesRemaining = Math.floor(secondsRemaining / 60D);
        if (secondsRemaining <= 0) {
            Bukkit.getOnlinePlayers().forEach(
                p -> p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You have been healed!"));
        }
        if (minutesRemaining > 0 && (minutesRemaining % 15) == 0) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(
                Chat.message("Full Heal", "You will be healed in {time}","{time}",
                    Time.INSTANCE.format(0, (long) msRemaining)).toLegacyText()));
        }
    }
}
