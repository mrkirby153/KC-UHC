package com.mrkirby153.kcuhc.game.team;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.discord.DiscordModule;
import com.mrkirby153.kcuhc.game.spectator.SpectatorInventory;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcutils.gui.Inventory;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class SpectatorTeam extends ScoreboardTeam {

    private UHC uhc;

    public SpectatorTeam(UHC uhc) {
        super("Spectators", ChatColor.GRAY);
        setFriendlyFire(false);
        setSeeInvisible(true);
        this.uhc = uhc;
    }

    @Override
    public void removePlayer(Player player) {
        super.removePlayer(player);
        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(uhc, player));
        getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
            .forEach(p -> player.hidePlayer(uhc, p));
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        if (player.getGameMode() == GameMode.SURVIVAL) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        Inventory<? extends JavaPlugin> openInventory = Inventory.Companion
            .getOpenInventory(player);
        if (openInventory != null && openInventory instanceof SpectatorInventory) {
            openInventory.close();
        }
        ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(m -> m.removeSpectator(player.getUniqueId()));
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(player);
        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(uhc, player));
        getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> {
            player.showPlayer(uhc, p);
            p.showPlayer(uhc, player);
        });
        player
            .addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));
        player
            .addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));
        player.setAllowFlight(true);
        player.setFlying(true);
        new SpectatorInventory(uhc, player);
        ModuleRegistry.INSTANCE.getLoadedModule(DiscordModule.class).ifPresent(m -> m.addSpectator(player.getUniqueId()));
    }
}
