package me.mrkirby153.kcuhc.gui;

import me.mrkirby153.kcuhc.item.ExecutableItem;
import me.mrkirby153.kcuhc.item.HotbarInventory;
import me.mrkirby153.kcuhc.item.InventoryHandler;
import me.mrkirby153.kcuhc.item.UndropableItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpectateInventory extends HotbarInventory {

    @Override
    public List<ExecutableItem> getItems() {
        List<ExecutableItem> items = new ArrayList<>();
        Compass c = new Compass();
        items.add(c);
        for (int i = 1; i <= 8; i++) {
            items.add(new SpeedItem(i));
        }
        return items;
    }

    class Compass extends ExecutableItem implements UndropableItem {

        public Compass() {
            super(Material.COMPASS, (short) 0, 1, ChatColor.GOLD + "Right Click to teleport to a player", null, Action.RIGHT_CLICK);
        }

        @Override
        public void execute(Player player, Action action) {
            InventoryHandler.instance().showInventory(player, new SpectateGUI());
        }
    }

    class SpeedItem extends ExecutableItem implements UndropableItem {

        private int level;

        public SpeedItem(int level) {
            super(Material.ARROW, (short) 0, level, ChatColor.RED + "Right click to set your movement speed to " + level + "x normal", null, Action.RIGHT_CLICK);
            this.level = level;
            // foo
        }

        @Override
        public void execute(Player player, Action action) {
            float flySpeed = (float) (0.1 * level);
            player.setFlySpeed(flySpeed);
            float walkSpeed = (float) (0.1 * (level+1));
            player.setWalkSpeed(walkSpeed);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEMFRAME_REMOVE_ITEM, 1, 1f);
        }
    }
}
