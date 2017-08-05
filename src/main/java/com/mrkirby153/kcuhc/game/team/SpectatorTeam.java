package com.mrkirby153.kcuhc.game.team;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.spectator.SpectatorInventory;
import me.mrkirby153.kcutils.gui.Inventory;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class SpectatorTeam extends ScoreboardTeam {

    private UHC uhc;

    public SpectatorTeam(UHC uhc) {
        super("Spectators", ChatColor.GRAY);
        friendlyFire = false;
        seeInvisible = true;
        this.uhc = uhc;
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(player);
        Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(player));
        players.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> {
            player.showPlayer(p);
            p.showPlayer(player);
        });
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));
        player.setAllowFlight(true);
        player.setFlying(true);
        new SpectatorInventory(uhc, player);
    }

    @Override
    public void removePlayer(Player player) {
        super.removePlayer(player);
        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(player));
        players.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(player::hidePlayer);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        if (player.getGameMode() == GameMode.SURVIVAL) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        if (Inventory.getOpenInventory(player) != null && Inventory.getOpenInventory(player) instanceof SpectatorInventory) {
            Inventory.getOpenInventory(player).close();
        }
    }
}
