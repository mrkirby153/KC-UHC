package me.mrkirby153.kcuhc.team;


import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.gui.SpecInventory;
import me.mrkirby153.kcuhc.shop.Inventory;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandAssignSpectator;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class TeamSpectator extends UHCTeam {

    private TeamHandler teamHandler;
    public TeamSpectator(TeamHandler teamHandler) {
        super(TeamHandler.SPECTATOR_TEAM_NAME, ChatColor.GRAY);
        this.teamHandler = teamHandler;
        setSeeInvisible(true);
        setFriendlyFire(false);
    }

    public void hideFromPlayers(Player player) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hidePlayer(player);
        }
        for (UUID u : teamHandler.spectatorsTeam().getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                player.showPlayer(p);
                p.showPlayer(player);
            }
        }
    }

    @Override
    public void onJoin(Player player) {
        player.setGlowing(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
        if (UHC.getInstance().arena.currentState() == UHCArena.State.RUNNING)
            new SpecInventory(UHC.getInstance(), player, teamHandler);
        if (UHC.uhcNetwork != null) {
            System.out.println("Giving " + player.getName() + " the spectator role");
            new BotCommandAssignSpectator(UHC.getInstance().serverId(), player.getUniqueId()).publish();
        }
        hideFromPlayers(player);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setCollidable(false);
    }

    @Override
    public void onLeave(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        Inventory.closeInventory(player);
        showToPlayers(player);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.2F);
        if (UHC.getInstance().arena != null && (UHC.getInstance().arena.currentState() == UHCArena.State.WAITING || UHC.getInstance().arena.currentState() == UHCArena.State.INITIALIZED
                || UHC.getInstance().arena.currentState() == UHCArena.State.ENDGAME))
            player.setAllowFlight(true);
        Inventory.closeInventory(player);
        player.setCollidable(true);
    }

    public void showToPlayers(Player player) {
        // Show the player to everyone
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(player);
        }
        // Hide all the spectators
        for (UUID u : UHC.getInstance().teamHandler.spectatorsTeam().getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                player.hidePlayer(p);
            }
        }
    }
}
