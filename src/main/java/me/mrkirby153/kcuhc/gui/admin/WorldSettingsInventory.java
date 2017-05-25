package me.mrkirby153.kcuhc.gui.admin;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.format.FormatKey;
import me.mrkirby153.kcutils.gui.Gui;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class WorldSettingsInventory extends Gui<UHC> {

    public WorldSettingsInventory(UHC plugin, Player player) {
        super(plugin, player, (int) Math.max(Math.ceil(getWorlds().length / 9D), 1), "World Settings");
    }

    private static String[] getWorlds() {
        File[] files = Bukkit.getWorldContainer().listFiles(pathname -> pathname != null && pathname.isDirectory()
                && pathname.getName().startsWith("UHC_") && !(pathname.getName().contains("nether") || pathname.getName().contains("end")));
        ArrayList<String> worlds = new ArrayList<>();
        if (files != null) {
            worlds.addAll(Arrays.stream(files).map(File::getName).collect(Collectors.toList()));
        }
        return worlds.toArray(new String[0]);
    }

    @Override
    public void build() {
        getInventory().clear();
        String[] worlds = getWorlds();
        for (int slot = 0; slot < worlds.length; slot++) {
            String world = worlds[slot];
            ItemFactory factory = new ItemFactory(Material.STAINED_CLAY);
            factory.name("World: " + ChatColor.GOLD + world);
            if (plugin.uhcWorld.getName().equalsIgnoreCase(world)) {
                factory.data(DyeColor.LIME.getWoolData());
            } else {
                factory.data(DyeColor.WHITE.getWoolData());
            }
            factory.lore(ChatColor.GRAY + "Left-Click to set world", ChatColor.GRAY + "Right-Click to delete world");
            addButton(slot, factory.construct(), (player, clickType) -> {
                if (clickType == ClickType.RIGHT) {
                    UHC.getInstance().multiWorldHandler.deleteUHCWorld(world);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1F, 1F);
                    player.sendMessage(C.m("World deleted!").toLegacyText());
                    build();
                }
                if (clickType == ClickType.LEFT) {
                    UHC.getInstance().multiWorldHandler.setWorld(world.replace("UHC_", ""));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
                    player.spigot().sendMessage(C.m("", "Set UHC world to {world}", new FormatKey("world", world)));
                    build();
                }
            });
        }
        addButton(worlds.length, new ItemFactory(Material.EMERALD_BLOCK).name("Create").construct(), (player, clickType) -> {
            close();
            player.spigot().sendMessage(C.m("Creating new world..."));
            UHC.getInstance().multiWorldHandler.createWorld();
            player.spigot().sendMessage(C.m("World created!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
            new WorldSettingsInventory(plugin, player).open();
        });
    }
}
