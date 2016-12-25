package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.gui.PropertyGui;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.worldborder.WorldBorderWarning;
import me.mrkirby153.kcuhc.module.player.LoneWolfModule;
import me.mrkirby153.kcuhc.module.player.PvPGraceModule;
import me.mrkirby153.kcuhc.module.player.SpreadPlayersModule;
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
        ArenaProperties properties = UHC.getInstance().arena.getProperties();

        if(ModuleRegistry.isLoaded(PvPGraceModule.class)) {
            integerProperty(properties.PVP_GRACE_MINS, Material.DIAMOND_HELMET, "PvP Grace (mins)", 4, 1, 3);
        }
        if(ModuleRegistry.isLoaded(SpreadPlayersModule.class)){
            integerProperty(properties.MIN_DISTANCE_BETWEEN_TEAMS, Material.PISTON_BASE, "Min Distance between teams (blocks)", 13);
        }

        if(ModuleRegistry.isLoaded(WorldBorderWarning.class)){
            integerProperty(properties.WORLDBORDER_WARN_DISTANCE, Material.NOTE_BLOCK, "World Border Warn Distance", 22);
        }

        if(ModuleRegistry.isLoaded(LoneWolfModule.class)) {
            addButton(37, new ItemFactory(Material.APPLE).name("Lone Wolf Settings").construct(), (player, clickType) -> {
                new LoneWolfSettingsInventory(plugin, player);
            });
        }
        addButton(39, new ItemFactory(Material.DIAMOND_SWORD).name("Team Settings").construct(), (player, clickType) -> new TeamSelectInventory(plugin, plugin.teamHandler, player));

        addButton(45, new ItemFactory(Material.ARROW).name("Back").construct(), (player, clickType) -> new GameAdminInventory(plugin, player));

        addButton(53, new ItemFactory(Material.BARRIER).name("World Border Settings").construct(), (player, clickType) -> new WorldborderSettingsInventory(plugin, player));

        addButton(45, new ItemFactory(Material.ARROW).name("Back").construct(), ((player, clickType) -> new GameAdminInventory(plugin, player)));
    }

}
