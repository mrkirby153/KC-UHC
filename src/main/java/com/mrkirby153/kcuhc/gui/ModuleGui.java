package com.mrkirby153.kcuhc.gui;

import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.module.ModuleRegistry;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.gui.Gui;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleGui extends Gui<UHC> {

    public ModuleGui(UHC uhc, Player player) {
        super(uhc, player, (int) Math.ceil((ModuleRegistry.INSTANCE.availableModules().size() + 1) / 9D), "Module Settings");
    }

    @Override
    public void build() {
        clear();
        int slot = 0;
        List<UHCModule> uhcModules = new ArrayList<>(ModuleRegistry.INSTANCE.availableModules());
        uhcModules.sort(Comparator.comparing(UHCModule::getName));
        for(UHCModule module : uhcModules){
            ItemFactory factory = new ItemFactory(module.getGuiItem()).data(module.getDamage());
            if (module.isLoaded())
                factory.glowing();

            factory.name(module.getName());

            factory.lore(module.isLoaded() ? ChatColor.GREEN + "LOADED" : ChatColor.RED + "UNLOADED");
            if (!module.getDescription().isEmpty())
                factory.lore("", module.getDescription());

            factory.lore("", "Left-Click to load", "Right-Click to unload");

            addButton(slot++, factory.construct(), (player, clickType) -> {
                if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
                    if (!ModuleRegistry.INSTANCE.loaded(module.getClass())) {
                        ModuleRegistry.INSTANCE.load(module);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
                        build();
                    }
                }
                if(clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT){
                    if(ModuleRegistry.INSTANCE.loaded(module.getClass())){
                        ModuleRegistry.INSTANCE.unload(module);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
                        build();
                    }
                }
            });
        }
    }
}
