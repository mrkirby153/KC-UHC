package me.mrkirby153.kcuhc.arena;


import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.gui.SpectateInventory;
import me.mrkirby153.kcuhc.item.InventoryHandler;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_9_R2.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class TeamSpectator extends UHCTeam {

    public TeamSpectator() {
        super(TeamHandler.SPECTATORS_TEAM, ChatColor.GRAY);
    }

    @Override
    public void onJoin(Player player) {
        player.setGlowing(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
        if (UHC.arena.currentState() == UHCArena.State.RUNNING)
            InventoryHandler.instance().showHotbar(player, new SpectateInventory());
        hideFromPlayers(player);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        ep.collides = false;
    }

    public void hideFromPlayers(Player player) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hidePlayer(player);
        }
        for (UUID u : TeamHandler.spectatorsTeam().getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                player.showPlayer(p);
                p.showPlayer(player);
            }
        }
    }

    @Override
    public void onLeave(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        InventoryHandler.instance().removeHotbar(player);
        showToPlayers(player);
        player.setAllowFlight(false);
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.2F);
        if(UHC.arena != null || UHC.arena.currentState() == UHCArena.State.WAITING || UHC.arena.currentState() == UHCArena.State.INITIALIZED
                || UHC.arena.currentState() == UHCArena.State.ENDGAME)
            player.setAllowFlight(true);
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        ep.collides = true;
    }

    public void showToPlayers(Player player) {
        // Show the player to everyone
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(player);
        }
        // Hide all the spectators
        for (UUID u : TeamHandler.spectatorsTeam().getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                player.hidePlayer(p);
            }
        }
    }
}
