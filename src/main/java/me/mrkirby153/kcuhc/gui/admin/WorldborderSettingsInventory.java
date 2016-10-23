package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.gui.PropertyGui;
import me.mrkirby153.kcuhc.shop.item.ShopItem;
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
        addButton(4, new ShopItem(Material.BARRIER, "Worldborder Settings"), null);
        integerProperty(properties.WORLDBORDER_START_SIZE, Material.FENCE, "Worldborder Start Size", 13);
        integerProperty(properties.WORLDBORDER_TRAVEL_TIME, Material.WATCH, "Worldborder Travel Time (mins)", 22);
        integerProperty(properties.WORLDBORDER_END_SIZE, Material.NETHER_FENCE, "Worldborder End Size", 31);
        addButton(45, new ShopItem(Material.ARROW, "Back"), (player, clickType) -> {
            player.closeInventory();
            new GameSettingsInventory(module, player);
        });

        booleanButton(properties.ENABLE_WORLDBORDER_WARNING, new ShopItem(Material.NOTE_BLOCK, "Worldborder Warning"), 44);
    }

}
