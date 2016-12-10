package me.mrkirby153.kcuhc.gui;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.shop.Inventory;
import me.mrkirby153.kcuhc.shop.item.ShopItem;
import me.mrkirby153.kcuhc.team.TeamHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpecInventory extends Inventory<UHC> {

    private TeamHandler teamHandler;

    public SpecInventory(UHC module, Player player, TeamHandler teamHandler) {
        super(module, player);
        this.teamHandler = teamHandler;
        open();
    }

    @Override
    public void build() {
        clear();
        addItem(hotbarSlot(1), new ShopItem(Material.COMPASS, ChatColor.GREEN + "Spectate (Right Click)"), (player1, clickType) -> new CompassInventory(player1,teamHandler));
        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            addItem(hotbarSlot(9), new ShopItem(Material.ENDER_PEARL, ChatColor.RED + "Toggle Night Vision (Right Click)"),
                    (player, type) -> {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        build();
                    });
        } else {
            addItem(hotbarSlot(9), new ShopItem(Material.EYE_OF_ENDER, ChatColor.GREEN + "Toggle Night Vision (Right Click)"),
                    (player, type) -> {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                        build();
                    });
        }
    }
}
