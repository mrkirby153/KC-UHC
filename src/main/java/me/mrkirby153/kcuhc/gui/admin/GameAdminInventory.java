package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.module.ModuleGui;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.gui.Gui;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class GameAdminInventory extends Gui<UHC> {

    public GameAdminInventory(UHC module, Player player) {
        super(module, player, 3, "Kirbycraft UHC");
        open();
    }

    @Override
    public void build() {
        addButton(12, new ItemFactory(Material.EMERALD_BLOCK).name(ChatColor.GREEN + "Start Countdown").lore("", "Click to start the countdown").construct(), (player, clickType) -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
            plugin.arena.startCountdown();
            player.closeInventory();
        });
        addButton(13, new ItemFactory(Material.GOLDEN_APPLE).data(1).name("Kirbycraft UHC").construct(), null);
        addButton(14, new ItemFactory(Material.REDSTONE_BLOCK).name(ChatColor.RED + "Stop Game").lore("", "Click to stop the game").construct(), (player, clickType) -> {
            plugin.arena.stop("Nobody", Color.WHITE);
            player.closeInventory();
        });

        addButton(26, new ItemFactory(Material.ANVIL).name("Configure Game").construct(), (player, clickType) -> new GameSettingsInventory(plugin, player));
        addButton(25, new ItemFactory(Material.BEACON).name("Modules").construct(), ((player, clickType) -> new ModuleGui(plugin, player)));
    }
}
