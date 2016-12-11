package me.mrkirby153.kcuhc.gui;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.shop.Inventory;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.ItemFactory;
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
        addItem(hotbarSlot(1), new ItemFactory(Material.COMPASS).name("Spectate " + ChatColor.GREEN + "(Right Click)").construct(), (player1, clickType) -> new CompassInventory(module, player1, teamHandler));
        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            addItem(hotbarSlot(9), new ItemFactory(Material.ENDER_PEARL).name("Toggle Night Vision " + ChatColor.GREEN + "(Right Click)").construct(),
                    (player, type) -> {
                        player.spigot().sendMessage(C.m("Took night vision"));
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        build();
                    });
        } else {
            addItem(hotbarSlot(9), new ItemFactory(Material.EYE_OF_ENDER).name("Toggle Night Vision " + ChatColor.GREEN + "(Right Click)").construct(),
                    (player, type) -> {
                        player.spigot().sendMessage(C.m("Given you night vision"));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                        build();
                    });
        }
    }
}
