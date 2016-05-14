package me.mrkirby153.kcuhc.shop.item;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopItem extends ItemStack {

    protected String name;
    protected String[] lore;

    public ShopItem(Material material, byte damage, int amount, String name, String[] lore) {
        super(material, amount, damage);
        this.name = name;
        this.lore = lore;
        update();
    }

    public ShopItem(Material material, int amount, String name, String[] lore) {
        this(material, (byte) 0, amount, name, lore);
    }

    public ShopItem(Material material, String name, String[] lore) {
        this(material, (byte) 0, 1, name, lore);
    }

    public ShopItem(Material material, byte damage, String name) {
        this(material, damage, 1, name, new String[0]);
    }

    public ShopItem(Material material, String name) {
        this(material, (byte) 0, 1, name, new String[0]);
    }


    public String getName() {
        return name;
    }

    public String[] getLore() {
        return lore;
    }

    public void glow() {
        addEnchantment(new NullEnchantment(), 1);
    }

    protected void update() {
        ItemMeta meta = this.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + getName());
        List<String> lore = new ArrayList<>();
        for (String s : getLore()) {
            lore.add(ChatColor.RESET + s);
        }
        meta.setLore(lore);
        this.setItemMeta(meta);
    }
}
