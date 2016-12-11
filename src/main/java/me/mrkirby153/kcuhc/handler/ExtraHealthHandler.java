package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.Module;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.UUID;

public class ExtraHealthHandler extends Module<UHC> implements Runnable, Listener {

    private HashMap<UUID, Integer> health = new HashMap<>();
    private HashMap<UUID, Double> savedHealth = new HashMap<>();

    public ExtraHealthHandler(UHC plugin) {
        super("Extra Health", "1.0", plugin);
    }

    public void addHeartRow(Player player) {
        int rows = 1;
        if (health.containsKey(player.getUniqueId())) {
            rows = health.remove(player.getUniqueId()) + 1;
        }
        health.put(player.getUniqueId(), rows);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void death(PlayerDeathEvent event) {
        event.getEntity().removePotionEffect(PotionEffectType.HEALTH_BOOST);
        health.remove(event.getEntity().getUniqueId());
        savedHealth.remove(event.getEntity().getUniqueId());
    }

    public void removeHealthRow(Player player) {
        int newRows = health.remove(player.getUniqueId()) - 1;
        if (newRows <= 0) {
            player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
            return;
        }
        health.put(player.getUniqueId(), newRows);
    }

    @Override
    public void run() {
        health.keySet().stream().map(Bukkit::getPlayer).filter(p -> p != null).filter(p -> !rightHealthBoost(p, health.get(p.getUniqueId()))).forEach(p -> {
            p.removePotionEffect(PotionEffectType.HEALTH_BOOST);
            int healthBoostRequired = getHealthBoostRequired(health.get(p.getUniqueId()));
            PotionEffect eff = null;
            for (PotionEffect e : p.getActivePotionEffects()) {
                if (e.getType() == PotionEffectType.HEALTH_BOOST) {
                    eff = e;
                    break;
                }
            }
            if (eff != null) {
                healthBoostRequired += eff.getAmplifier();
            }
            if (healthBoostRequired != -1) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, getHealthBoostRequired(health.get(p.getUniqueId())), true, false));
            }
            if (savedHealth.containsKey(p.getUniqueId())) {
                Double v = savedHealth.get(p.getUniqueId());
                if (v > p.getMaxHealth())
                    v = p.getMaxHealth();
                p.setHealth(v);
            }
        });
        health.keySet().stream().map(Bukkit::getPlayer).filter(p -> p != null).forEach(p -> savedHealth.put(p.getUniqueId(), p.getHealth()));
    }

    private int getHealthBoostRequired(int rowsOfHearts) {
        return (5 * rowsOfHearts) - 1;
    }

    private boolean rightHealthBoost(Player player, int rows) {
        int healthShouldHave = 20 + (20 * rows);
        double health = Math.floor(player.getMaxHealth());
        return healthShouldHave == health;
    }

    @Override
    protected void init() {
        registerListener(this);
        scheduleRepeating(this, 0L, 3L);
    }
}
