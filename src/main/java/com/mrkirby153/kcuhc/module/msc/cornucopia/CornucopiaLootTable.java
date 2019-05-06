package com.mrkirby153.kcuhc.module.msc.cornucopia;

import me.mrkirby153.kcutils.ItemFactory;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CornucopiaLootTable {

    private ArrayList<LootItem> loot = new ArrayList<>();

    private Random random = new Random();

    public CornucopiaLootTable() {
        addLoot(new ItemFactory(Material.DIAMOND).construct(), 70, 2);
        addLoot(new ItemFactory(Material.DIAMOND_SWORD).construct(), 90, 1, true);
        addLoot(new ItemFactory(Material.STICK).construct(), 10, 3);
        addLoot(new ItemFactory(Material.DIAMOND_CHESTPLATE)
            .enchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1).construct(), 95, 1, true);
        addLoot(new ItemFactory(Material.COBBLESTONE).construct(), 10, 10);
        addLoot(new ItemFactory(Material.IRON_INGOT).construct(), 45, 2);
        addLoot(new ItemFactory(Material.GOLDEN_APPLE).construct(), 0, 3, true);
        addLoot(new ItemFactory(Material.BEDROCK).construct(), 90, 30);
        addLoot(new ItemFactory(Material.DANDELION_YELLOW).construct(), 60, 4);
        addLoot(new ItemFactory(Material.BOW).enchantment(Enchantment.KNOCKBACK, 30).name("POW!")
            .damage(382).construct(), 90, 1, true);
        addLoot(new ItemFactory(Material.EXPERIENCE_BOTTLE).construct(), 0, 32, true);
        addLoot(new ItemFactory(Material.FLINT_AND_STEEL).damage(52).construct(), 60, 1, true);
        addLoot(new ItemFactory(Material.LAVA_BUCKET).construct(), 70, 1, true);
        addLoot(new ItemFactory(Material.TNT).construct(), 80, 10);
        addLoot(new ItemFactory(Material.GOLD_INGOT).construct(), 75, 8);
        addLoot(new ItemFactory(Material.APPLE).construct(), 30, 3);
    }

    public void addLoot(ItemStack stack, int probability, int maxStackSize, boolean oneOnly) {
        loot.add(new LootItem(stack, probability, oneOnly, maxStackSize));
    }

    public void addLoot(ItemStack stack, int probability) {
        addLoot(stack, probability, 1, false);
    }

    public void addLoot(ItemStack stack, int probability, int maxStackSize) {
        addLoot(stack, probability, maxStackSize, false);
    }

    public List<ItemStack> get(int maxItems) {
        List<ItemStack> toReturn = new ArrayList<>();

        List<LootItem> oneOnly = new ArrayList<>();

        int items = random.nextInt(maxItems);

        do {
            loot.forEach(lootItem -> {
                double number = random.nextDouble() * 100;
                if (lootItem.probability <= number) {
                    if (lootItem.oneOnly && oneOnly.contains(lootItem)) {
                        return;
                    } else {
                        oneOnly.add(lootItem);
                    }
                    ItemStack itemStack = lootItem.itemStack;
                    itemStack.setAmount(Math.max(1, random.nextInt(lootItem.maxStackSize)));
                    toReturn.add(itemStack);
                }
            });
        } while (toReturn.size() < items);

        return toReturn;
    }

    private class LootItem {

        private final ItemStack itemStack;
        private final int probability;
        private final boolean oneOnly;

        private int maxStackSize = 1;

        private LootItem(ItemStack itemStack, int probability, boolean oneOnly, int maxStackSize) {
            this.itemStack = itemStack;
            this.probability = probability;
            this.oneOnly = oneOnly;
            this.maxStackSize = maxStackSize;
        }
    }
}
