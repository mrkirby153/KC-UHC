package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.shop.Shop;
import me.mrkirby153.kcuhc.shop.item.ShopItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class GameAdminInventory extends Shop<UHC> {

    public GameAdminInventory(UHC module, Player player) {
        super(module, player, 3, "Kirbycraft UHC");
        open();
    }

    @Override
    public void build() {
        addButton(12, new ShopItem(Material.EMERALD_BLOCK, 1, ChatColor.GREEN+"Start Game", new String[]{
                "Click to start the countdown"
        }), (player, clickType) -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
            UHC.arena.startCountdown();
            player.closeInventory();
        });
        addButton(13, new ShopItem(Material.GOLDEN_APPLE, (byte) 1, "Kirbycraft UHC"), null);
        addButton(14, new ShopItem(Material.REDSTONE_BLOCK, 1, ChatColor.RED+"Stop Game", new String[]{
                "Click to stop the game"
        }), (player, clickType) -> {
            UHC.arena.stop("Nobody");
            player.closeInventory();
        });

        addButton(26, new ShopItem(Material.ANVIL, "Configure Game"), ((player, clickType) -> {
            player.closeInventory();
            new GameSettingsInventory(module, player);
        }));
    }
}
