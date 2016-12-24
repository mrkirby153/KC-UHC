package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.gui.PropertyGui;
import me.mrkirby153.kcutils.ItemFactory;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class LoneWolfSettingsInventory extends PropertyGui {

    public LoneWolfSettingsInventory(UHC plugin, Player player) {
        super(plugin, player, 4, "Lone Wolf Settings");
        open();
    }

    @Override
    public void build() {
        ArenaProperties properties = UHC.getInstance().arena.getProperties();

        booleanButton(properties.LONE_WOLF_CREATES_TEAMS, new ItemFactory(Material.APPLE).name("Create Teams").construct(), 4);
        integerProperty(properties.LONE_WOLF_TEAM_SIZE, Material.SLIME_BALL, "Lone Wolf Max Team Size", 22, 1, 3);
        addButton(27, new ItemFactory(Material.ARROW).name("Back").construct(), ((player, clickType) -> {
            new TeamSelectInventory(plugin, UHC.getInstance().teamHandler, player);
        }));
    }
}
