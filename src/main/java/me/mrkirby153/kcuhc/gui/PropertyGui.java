package me.mrkirby153.kcuhc.gui;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.gui.Action;
import me.mrkirby153.kcutils.gui.Gui;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class PropertyGui extends Gui<UHC> {
    public PropertyGui(UHC module, Player player, int rows, String title) {
        super(module, player, rows, title);
    }

    private ItemStack makeItem(int count) {
        return new ItemFactory(Material.QUARTZ_BLOCK).name(count > 0 ? "+" + count : "" + count).construct();
    }

    protected void booleanButton(ArenaProperties.Property<Boolean> property, ItemStack item, int slot) {
        addButton(slot, item, null);
        ItemStack toggleItem = new ItemFactory(Material.INK_SACK).data(property.get() ? 10 : 8).name(property.get() ? "Enabled" : "Disabled").construct();
        addButton(slot + 9, toggleItem, (player, clickType) -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
            property.setValue(!property.get());
            build();
        });
    }

    protected void integerProperty(ArenaProperties.Property<Integer> property, Material item, String name, int slot, int... numbers) {
        int current = property.get();

        // -100 -10 -1
        addButton(slot, new ItemFactory(item).name(name + ": " + ChatColor.GOLD + current).construct(), null);
        for (int i = 0; i < numbers.length; i++) {
            addButton(slot - (i + 1), makeItem(numbers[i] * -1), new IntegerAction(property, numbers[i] * -1));
            addButton(slot + (i + 1), makeItem(numbers[i]), new IntegerAction(property, numbers[i]));
        }
    }

    protected void integerProperty(ArenaProperties.Property<Integer> property, Material item, String name, int slot) {
        integerProperty(property, item, name, slot, 1, 10, 100);
    }

    private class IntegerAction implements Action {

        private final ArenaProperties.Property<Integer> prop;
        private final int count;

        private IntegerAction(ArenaProperties.Property<Integer> prop, int count) {
            this.prop = prop;
            this.count = count;
        }


        @Override
        public void onClick(Player player, ClickType clickType) {
            prop.setValue(prop.get() + count);
            if (prop.get() < 0)
                prop.setValue(0);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
            build();
        }
    }
}
