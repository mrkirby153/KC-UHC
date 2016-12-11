package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.gui.PropertyGui;
import me.mrkirby153.kcutils.ItemFactory;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WorldborderSettingsInventory extends PropertyGui {
    public WorldborderSettingsInventory(UHC module, Player player) {
        super(module, player, 6, "Worldborder Settings");
        open();
    }

    @Override
    public void build() {
        ArenaProperties properties = UHC.arena.getProperties();
        addButton(4, new ItemFactory(Material.BARRIER).name("World Border Settings").construct(), null);
        integerProperty(properties.WORLDBORDER_START_SIZE, Material.FENCE, "Worldborder Start Size", 13);
        integerProperty(properties.WORLDBORDER_TRAVEL_TIME, Material.WATCH, "Worldborder Travel Time (mins)", 22);
        integerProperty(properties.WORLDBORDER_END_SIZE, Material.NETHER_FENCE, "Worldborder End Size", 31);
        addButton(45, new ItemFactory(Material.ARROW).name("Back").construct(), (player, clickType) -> {
            player.closeInventory();
            new GameSettingsInventory(plugin, player);
        });

        booleanButton(properties.ENABLE_WORLDBORDER_WARNING, new ItemFactory(Material.NOTE_BLOCK).name("World Border Warning").construct(), 44);
    }

}
