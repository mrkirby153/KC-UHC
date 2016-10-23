package me.mrkirby153.kcuhc.gui;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.ArenaProperties;
import me.mrkirby153.kcuhc.shop.Shop;
import me.mrkirby153.kcuhc.shop.item.Action;
import me.mrkirby153.kcuhc.shop.item.ShopItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public abstract class PropertyGui extends Shop<UHC> {
    public PropertyGui(UHC module, Player player, int rows, String title) {
        super(module, player, rows, title);
    }

    protected void booleanButton(ArenaProperties.Property<Boolean> property, ShopItem item, int slot) {
        addButton(slot, item, null);
        ShopItem toggleItem;
        if (property.get()) {
            toggleItem = new ShopItem(Material.INK_SACK, (byte) 10, 1, "Enabled", new String[0]);
        } else {
            toggleItem = new ShopItem(Material.INK_SACK, (byte) 8, 1, "Disabled", new String[0]);
        }
        addButton(slot + 9, toggleItem, (player, clickType) -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
            property.setValue(!property.get());
            build();
        });
    }

    protected void integerProperty(ArenaProperties.Property<Integer> property, Material item, String name, int slot) {
        int current = property.get();
        ShopItem i = new ShopItem(item, name + ": " + ChatColor.GOLD + current);

        // -100 -10 -1
        addButton(slot, i, null);
        addButton(slot - 1, makeItem(-1), new IntegerAction(property, -1));
        addButton(slot - 2, makeItem(-10), new IntegerAction(property, -10));
        addButton(slot - 3, makeItem(-100), new IntegerAction(property, -100));

        // +1 +10 +100
        addButton(slot + 1, makeItem(1), new IntegerAction(property, 1));
        addButton(slot + 2, makeItem(10), new IntegerAction(property, 10));
        addButton(slot + 3, makeItem(100), new IntegerAction(property, 100));
    }

    private ShopItem makeItem(int count) {
        return new ShopItem(Material.QUARTZ_BLOCK, (count > 0 ? "+" : "") + count);
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
