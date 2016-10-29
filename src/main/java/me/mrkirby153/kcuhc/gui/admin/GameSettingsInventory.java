package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.gui.PropertyGui;
import me.mrkirby153.kcuhc.shop.item.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class GameSettingsInventory extends PropertyGui {

    public GameSettingsInventory(UHC module, Player player) {
        super(module, player, 6, "Configure Game");
        open();
    }

    @Override
    public void build() {
        ArenaProperties properties = UHC.arena.getProperties();
        booleanButton(properties.SPREAD_PLAYERS, new ShopItem(Material.ENDER_PEARL, "Spread Players"), 0);
        booleanButton(properties.CHECK_ENDING, new ShopItem(Material.CAKE, "End Check"), 2);
        booleanButton(properties.DROP_PLAYER_HEAD, new ShopItem(Material.SKULL_ITEM, (byte) 3, 1, "Drop Player Heads", new String[0]), 4);
        booleanButton(properties.ENABLE_HEAD_APPLE, new ShopItem(Material.GOLDEN_APPLE, "Head Apples"), 6);
        booleanButton(properties.GIVE_COMPASS_ON_START, new ShopItem(Material.COMPASS, "Give Compass on Start"), 8);

        booleanButton(properties.TEAM_INV_ENABLED, new ShopItem(Material.CHEST, "Team Inventories"), 27);
        booleanButton(properties.COMPASS_PLAYER_TRACKER, new ShopItem(Material.EYE_OF_ENDER, "Compass Tracks Players"), 29);
        booleanButton(properties.ENABLE_ENDGAME, new ShopItem(Material.TNT, "Endgame"), 31);
        booleanButton(properties.REGEN_TICKET_ENABLE, new ShopItem(Material.PAPER, "Regen Tickets"), 33);

        addButton(45, new ShopItem(Material.ARROW, "Back"), (player, clickType) -> {
            player.closeInventory();
            new GameAdminInventory(module, player);
        });

        addButton(53, new ShopItem(Material.BARRIER, "Worldborder Settings"), (player, clickType) -> {
            player.closeInventory();
            new WorldborderSettingsInventory(module, player);
        });

        addButton(45, new ShopItem(Material.DIAMOND_SWORD, "Team Settings"), (player, clickType) -> {
            player.closeInventory();
            new TeamSelectInventory(module, player);
        });
        integerProperty(properties.PVP_GRACE_MINS, Material.DIAMOND_HELMET, "PvP Grace (mins)", 49, 1, 3);
    }

}
