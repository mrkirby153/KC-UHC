package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.gui.PropertyGui;
import me.mrkirby153.kcutils.ItemFactory;
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
        booleanButton(properties.SPREAD_PLAYERS, new ItemFactory(Material.ENDER_PEARL).name("Spread Players").construct(), 0);
        booleanButton(properties.CHECK_ENDING, new ItemFactory(Material.CAKE).name("End Check").construct(), 2);
        booleanButton(properties.DROP_PLAYER_HEAD, new ItemFactory(Material.SKULL_ITEM).data(3).name("Drop Player Heads").construct(), 4);
        booleanButton(properties.ENABLE_HEAD_APPLE, new ItemFactory(Material.GOLDEN_APPLE).name("Head Apples").construct(), 6);
        booleanButton(properties.GIVE_COMPASS_ON_START, new ItemFactory(Material.COMPASS).name("Give Compass on Start").construct(), 8);

        booleanButton(properties.TEAM_INV_ENABLED,new ItemFactory(Material.CHEST).name("Team Inventories").construct(), 27);
        booleanButton(properties.COMPASS_PLAYER_TRACKER, new ItemFactory(Material.EYE_OF_ENDER).name("Compass Tracks Players").construct(), 29);
        booleanButton(properties.ENABLE_ENDGAME, new ItemFactory(Material.TNT).name("Endgame").construct(), 31);
        booleanButton(properties.REGEN_TICKET_ENABLE, new ItemFactory(Material.PAPER).name("Regen Tickets").construct(), 33);

        addButton(45, new ItemFactory(Material.ARROW).name("Back").construct(), (player, clickType) -> {
            player.closeInventory();
            new GameAdminInventory(plugin, player);
        });

        addButton(53, new ItemFactory(Material.BARRIER).name("World Border Settings").construct(), (player, clickType) -> {
            player.closeInventory();
            new WorldborderSettingsInventory(plugin, player);
        });

        addButton(45, new ItemFactory(Material.DIAMOND_SWORD).name("Team Settings").construct(), (player, clickType) -> {
            player.closeInventory();
            new TeamSelectInventory(plugin, UHC.plugin.teamHandler, player);
        });
        integerProperty(properties.PVP_GRACE_MINS, Material.DIAMOND_HELMET, "PvP Grace (mins)", 49, 1, 3);
    }

}
