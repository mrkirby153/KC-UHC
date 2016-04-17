package me.mrkirby153.kcuhc.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public abstract class ExecutableItem {

    protected  final Material material;
    protected final short damage;
    protected final Action action;

    protected String name;
    protected List<String> lore;

    private int inSlot = -1;
    protected int count;

    public ExecutableItem(Material material, short damage, int count, String name, List<String> lore, Action action) {
        this.material = material;
        this.damage = damage;
        this.action = action;
        this.name = name;
        this.lore = lore;
        this.count = count;
    }

    public ItemStack getItem(){
        ItemStack itemStack = new ItemStack(this.material, this.count, this.damage);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(this.name);
        meta.setLore(this.lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public void setSlot(int slot){
        this.inSlot = slot;
    }

    public int getSlot(){
        return this.inSlot;
    }

    public abstract void execute(Player player, Action action);

    public Action getAction(){
        return action;
    }

    public enum Action{
        NO_ACTION,
        LEFT_CLICK,
        RIGHT_CLICK,
        INV_CLICK;
    }
}
