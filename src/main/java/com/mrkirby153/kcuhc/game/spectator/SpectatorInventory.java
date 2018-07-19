package com.mrkirby153.kcuhc.game.spectator;

import com.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.gui.Inventory;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class SpectatorInventory extends Inventory<UHC> {

    public SpectatorInventory(UHC uhc, Player player) {
        super(uhc, player);
        open();
    }

    @Override
    public void build() {
        clear();
        addItem(hotbarSlot(1),
            new ItemFactory(Material.COMPASS).name("Spectate " + ChatColor.GREEN + "( Right Click)")
                .construct(), (player1, clickType) -> {
                new SpectatorGui(getPlugin()).open(player1);
            });
        if (getPlayer().getGameMode() != GameMode.SPECTATOR) {
            addItem(hotbarSlot(3), new ItemFactory(Material.BARRIER)
                    .name("Enter Vanilla Spectator" + ChatColor.GREEN + " (Right Click)").construct(),
                (player1, clickType) -> {
                    if (clickType == ClickType.RIGHT) {
                        player1.setGameMode(GameMode.SPECTATOR);
                        player1.spigot().sendMessage(Chat.message("Spectator",
                            "You have entered vanilla spectator mode. Type {spec} to return to survival",
                            "{spec}", "/spectate"));
                        build();
                    }
                });
        } else {
            addItem(hotbarSlot(3), new ItemFactory(Material.WOOL).data(DyeColor.RED.getWoolData())
                    .name("Return to Survival" + ChatColor.GREEN + "(Right Click)").construct(),
                (p, clickType) -> {
                    if (clickType == ClickType.RIGHT) {
                        p.setGameMode(GameMode.SURVIVAL);
                        p.setAllowFlight(true);
                        p.setFlying(true);
                        p.spigot().sendMessage(Chat
                            .message("Spectator", "You have returned to survival mode."));
                        build();
                    }
                });
        }
    }
}
