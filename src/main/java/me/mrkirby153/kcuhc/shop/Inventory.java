package me.mrkirby153.kcuhc.shop;


import me.mrkirby153.kcuhc.shop.item.Action;
import me.mrkirby153.kcuhc.shop.item.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public abstract class Inventory<T extends JavaPlugin> implements Listener {


    private static HashMap<Player, Inventory<? extends JavaPlugin>> playerInventoryHashMap = new HashMap<>();

    protected T module;

    private Map<Integer, Action> actions = new HashMap<>();
    private Map<ItemStack, Integer> stackToInventorySlotMap = new HashMap<>();
    protected Player player;

    private static final int MIN_ROWS = 1;
    private static final int MIN_COLUMNS = 1;
    private static final int MAX_ROWS = 3;
    private static final int MAX_COLUMNS = 9;
    private static final int ROW_OFFSET = 1;
    private static final int COLUMN_OFFSET = 1;
    private static final int COLUMNS_PER_ROW = 9;
    private static final int PLAYER_INV_START_SLOT = 9;

    public Inventory(T module, Player player) {
        this.module = module;
        this.player = player;
        registerListener();
        playerInventoryHashMap.put(player, this);
    }


    public abstract void build();

    public void onOpen() {

    }

    public void onClose() {

    }

    protected void open() {
        onOpen();
        build();
    }

    protected void close() {
        clear();
        playerInventoryHashMap.remove(player);
    }

    public static void closeInventory(Player player) {
        Inventory inv = playerInventoryHashMap.remove(player);
        if (inv != null)
            inv.close();
    }

    public void addItem(int slot, ShopItem item, Action action) {
        actions.put(slot, action);
        stackToInventorySlotMap.put(item, slot);
        player.getInventory().setItem(slot, item);
        player.updateInventory();
    }

    protected int hotbarSlot(int slot) {
        return --slot;
    }

    public void clear() {
        player.getInventory().clear();
        player.updateInventory();
    }

    protected int invRowCol(int row, int col) {
        if (row < MIN_ROWS || row > MAX_ROWS)
            throw new IllegalArgumentException("There are only 3 rows in a player inventory!");
        if (col < MIN_COLUMNS || col > MAX_COLUMNS)
            throw new IllegalArgumentException("There are only 9 columns in a player inventory!");
        int slot = ((row - ROW_OFFSET) * COLUMNS_PER_ROW);
        slot += (col - COLUMN_OFFSET);
        return slot + PLAYER_INV_START_SLOT;
    }

    private Action getAction(int slot) {
        return actions.get(slot);
    }

    private Action getAction(ItemStack itemStack) {
        int foundSlot = -1;
        for (Map.Entry<ItemStack, Integer> i : stackToInventorySlotMap.entrySet()) {
            if (i.getKey().equals(itemStack)) {
                foundSlot = i.getValue();
                break;
            }
        }
        if (foundSlot == -1)
            return null;
        return actions.get(foundSlot);
    }

    private void registerListener() {
        System.out.println("Registering events");
        module.getServer().getPluginManager().registerEvents(this, module);
    }

    private void unregisterListener() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if(event.getWhoClicked() != player)
            return;
        event.setCancelled(true);
        org.bukkit.inventory.Inventory clickedInventory = event.getClickedInventory();
        if(clickedInventory == null)
            return;
        if (clickedInventory.getType() == InventoryType.PLAYER) {
            Action a = getAction(event.getSlot());
            if (a != null) {
                a.onClick((Player) event.getWhoClicked(), event.getClick());
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void itemDropEvent(PlayerDropItemEvent event) {
        // Cancel all item drops while the player has the inventory open.
        // Maybe we should do some logic checking to see if the item is an action
        if (event.getPlayer() == player)
            event.setCancelled(true);
    }

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        if(event.getPlayer() != player)
            return;
        ClickType clickType = null;
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                if (event.getPlayer().isSneaking())
                    clickType = ClickType.SHIFT_RIGHT;
                else
                    clickType = ClickType.RIGHT;
                break;
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                if (event.getPlayer().isSneaking())
                    clickType = ClickType.SHIFT_LEFT;
                else
                    clickType = ClickType.LEFT;
                break;
        }
        if (clickType == null)
            return;
        event.setCancelled(true);
        Action a = getAction(event.getItem());
        if (a != null) {
            a.onClick(event.getPlayer(), clickType);
        }
    }

    @EventHandler
    public void logout(PlayerQuitEvent event) {
        close();
    }
}
