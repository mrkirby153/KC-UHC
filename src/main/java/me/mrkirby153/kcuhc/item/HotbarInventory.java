package me.mrkirby153.kcuhc.item;


import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public abstract class HotbarInventory implements ActionInventory {

    private HashMap<ItemStack, ExecutableItem> items = new HashMap<>();

    public void addItem(ExecutableItem item) {
        items.put(item.getItem(), item);
    }

    public ExecutableItem getExecItemForItem(ItemStack stack) {
        return items.get(stack);
    }

    @Override
    public String getName() {
        return "hotbar_inv";
    }

    @Override
    public int rows() {
        return 1;
    }
}
