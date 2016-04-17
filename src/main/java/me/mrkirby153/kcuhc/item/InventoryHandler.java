package me.mrkirby153.kcuhc.item;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class InventoryHandler implements Listener {

    private static InventoryHandler inst;
    private HashMap<UUID, ActionInventory> activeInventories = new HashMap<>();
    private HashMap<UUID, HotbarInventory> hotbarInvs = new HashMap<>();

    public static InventoryHandler instance() {
        if (inst == null)
            inst = new InventoryHandler();
        return inst;
    }

    public void showInventory(Player player, Gui gui) {
        activeInventories.put(player.getUniqueId(), gui);
        List<ExecutableItem> items = gui.getItems();
        Inventory inventory = Bukkit.createInventory(null, gui.rows() * 9, gui.getName());
        int slot = 0;
        for (ExecutableItem eI : items) {
            if (eI.getSlot() != -1) {
                inventory.setItem(eI.getSlot(), eI.getItem());
            } else {
                // Find next empty slot
                while (inventory.getItem(slot) != null)
                    slot++;
                eI.setSlot(slot);
                inventory.setItem(slot, eI.getItem());
            }
            gui.setStackInSlot(eI);
        }
        player.openInventory(inventory);
    }

    public void showHotbar(Player player, HotbarInventory inventory) {
        player.getInventory().clear();
        this.hotbarInvs.put(player.getUniqueId(), inventory);
        List<ExecutableItem> items = inventory.getItems();
        int slot = 0;
        for (ExecutableItem eI : items) {
            if (eI.getSlot() != -1)
                player.getInventory().setItem(eI.getSlot(), eI.getItem());
            else {
                while (player.getInventory().getItem(slot) != null && slot <= 8) {
                    slot++;
                }
                eI.setSlot(slot);
                player.getInventory().setItem(slot, eI.getItem());
            }
            inventory.addItem(eI);
        }
        player.updateInventory();
    }

    public void removeHotbar(Player player) {
        player.getInventory().clear();
        this.hotbarInvs.remove(player.getUniqueId());
    }

    public void closeInventory(Player player, boolean forceClose) {
        if (forceClose)
            player.closeInventory();
        this.activeInventories.remove(player.getUniqueId());
    }

    public void closeInventory(Player player) {
        closeInventory(player, true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        closeInventory((Player) event.getPlayer(), false);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != null) {
            HumanEntity player = event.getView().getPlayer();
            ActionInventory inv = activeInventories.get(player.getUniqueId());
            if (inv instanceof Gui) {
                Gui gui = (Gui) inv;
                ExecutableItem item = gui.getStackInSlot(event.getSlot());
                if (item != null) {
                    if (item.action == ExecutableItem.Action.INV_CLICK)
                        item.execute((Player) player, ExecutableItem.Action.INV_CLICK);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void playerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        HotbarInventory inventory = hotbarInvs.get(player.getUniqueId());
        if (inventory != null) {
            ExecutableItem eItem = inventory.getExecItemForItem(event.getItem());
            if (eItem != null) {
                event.setCancelled(true);
                ExecutableItem.Action a = null;
                switch (event.getAction()) {
                    case LEFT_CLICK_AIR:
                    case LEFT_CLICK_BLOCK:
                        a = ExecutableItem.Action.LEFT_CLICK;
                        break;
                    case RIGHT_CLICK_AIR:
                    case RIGHT_CLICK_BLOCK:
                        a = ExecutableItem.Action.RIGHT_CLICK;
                        break;
                }
                if (eItem.getAction() == a)
                    eItem.execute(player, a);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void undropItem(PlayerDropItemEvent event) {
        HotbarInventory inventory = hotbarInvs.get(event.getPlayer().getUniqueId());
        if (inventory != null) {
            ExecutableItem executableItem = inventory.getExecItemForItem(event.getItemDrop().getItemStack());
            if (executableItem instanceof UndropableItem)
                event.setCancelled(true);
        }
    }
}
