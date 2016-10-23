package me.mrkirby153.kcuhc.shop;

import me.mrkirby153.kcuhc.shop.item.Action;
import me.mrkirby153.kcuhc.shop.item.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public abstract class Shop<T extends JavaPlugin> implements Listener {

    protected T module;

    private Inventory inventory;
    private Player player;

    protected Map<Integer, Action> actions = new HashMap<>();

    public Shop(T module, Player player, int rows, String title) {
        this.inventory = Bukkit.createInventory(null, rows * 9, title);
        this.module = module;
        this.player = player;
    }

    public void onOpen() {

    }

    public abstract void build();

    protected final void addButton(int slot, ShopItem item, Action action) {
        actions.put(slot, action);
        getInventory().setItem(slot, item);
    }

    public final void inventoryClose() {
        HandlerList.unregisterAll(this);
        onClose();
    }

    public void onClose(){

    }

    protected final Inventory getInventory() {
        return inventory;
    }

    protected final void open(){
        onOpen();
        build();
        player.openInventory(getInventory());
        module.getServer().getPluginManager().registerEvents(this, module);
    }

    final Action getAction(int slot) {
        return actions.get(slot);
    }


    @EventHandler
    public void inventoryCloseEvent(InventoryCloseEvent event) {
        inventoryClose();
    }

    @EventHandler
    public void inventoryClickEvent(InventoryClickEvent event) {
        if(event.getClickedInventory() == null)
            return;
        if(!event.getClickedInventory().getName().equals(inventory.getName()))
            return;
        event.setCancelled(true);
        int slot = event.getSlot();
        ItemStack is = event.getView().getItem(event.getRawSlot());
        if(is.getType() == Material.AIR)
            actions.remove(slot);
        Action a = getAction(slot);
        if (a != null && event.getWhoClicked() instanceof Player) {
            a.onClick((Player) event.getWhoClicked(), event.getClick());
        }
    }
}
