package com.mrkirby153.kcuhc.game.spectator;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.gui.Inventory;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("spectate")
public class CommandSpectate extends BaseCommand {

    private UHC uhc;

    @Inject
    public CommandSpectate(UHC game) {
        this.uhc = game;
    }

    @Subcommand("inv")
    public void inventory(Player player) {
        new SpectatorGui(uhc).open(player);
    }

    @Default
    public void returnToSurvival(Player player) {
        if (uhc.getGame().getCurrentState() != GameState.ALIVE) {
            if (uhc.spectatorHandler.pendingSpectators.contains(player.getUniqueId())) {
                uhc.spectatorHandler.pendingSpectators.remove(player.getUniqueId());
                player.spigot().sendMessage(
                    Chat.message("Spectate", "You are no longer spectating this round"));
            } else {
                uhc.spectatorHandler.pendingSpectators.add(player.getUniqueId());
                player.spigot().sendMessage(
                    Chat.message("Spectate", "You are spectating this round"));
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F);
        } else {
            if (!uhc.getGame().isSpectator(player)) {
                player.spigot().sendMessage(Chat.error("You are not a spectator."));
                return;
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(true);
            player.setFlying(true);
            Inventory<? extends JavaPlugin> openInventory = Inventory.Companion
                .getOpenInventory(player);
            if (openInventory != null) {
                openInventory.build();
            }
        }
    }
}
