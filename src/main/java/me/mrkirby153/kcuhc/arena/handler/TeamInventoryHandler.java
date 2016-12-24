package me.mrkirby153.kcuhc.arena.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.Module;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TeamInventoryHandler extends Module<UHC> implements Listener, Runnable{

    private HashMap<UHCTeam, Inventory> teamInventories = new HashMap<>();
    private Set<ItemStack> teamInventoryItems = new HashSet<>();

    public TeamInventoryHandler(UHC uhc) {
        super("Team Inventory", "1.0", uhc);
    }

    public void dropInventory(UHCTeam team, Location location) {
        Inventory inv = teamInventories.remove(team);
        if (inv == null)
            return;
        for (ItemStack i : inv.getContents()) {
            if (i != null)
                location.getWorld().dropItemNaturally(location, i);
        }
    }

    public Inventory getInventory(UHCTeam team) {
        Inventory inv = teamInventories.get(team);
        if (inv == null) {
            String name = team.getFriendlyName();
            if (name == null)
                name = WordUtils.capitalizeFully(team.getTeamName().replace('_', ' '));
            teamInventories.put(team, inv = Bukkit.createInventory(null, 9 * 3, "Team Inventory: " + name));
        }
        return inv;
    }

    public void giveInventoryItem(Player player) {
        UHCTeam team = getPlugin().teamHandler.getTeamForPlayer(player);
        if (team == null)
            return;
        ItemStack inventoryStack = new ItemFactory(Material.ENDER_CHEST).name(ChatColor.WHITE + "Team Inventory: "
                + team.getColor() + team.getFriendlyName()).lore("Right click to open your team's inventory").construct();
        this.teamInventoryItems.add(inventoryStack);
        player.getInventory().addItem(inventoryStack);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (teamInventoryItems.contains(event.getItemInHand())) {
            UHCTeam team = getPlugin().teamHandler.getTeamForPlayer(event.getPlayer());
            event.setCancelled(true);
            if (team != null) {
                event.getPlayer().openInventory(getInventory(team));
            } else {
                takeInventoryItem(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (teamInventoryItems.contains(event.getItem())) {
                event.setCancelled(true);
                UHCTeam team = getPlugin().teamHandler.getTeamForPlayer(event.getPlayer());
                if (team != null) {
                    event.getPlayer().openInventory(getInventory(team));
                } else {
                    if (takeInventoryItem(event.getPlayer())) {
                        event.getPlayer().sendMessage(UtilChat.message("Your team inventory item has been removed! Use "
                                + ChatColor.GOLD + "/teaminv item" + "" + ChatColor.GRAY + " to re-add"));
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryClickEvent event) {
        if (teamInventoryItems.contains(event.getCurrentItem())) {
            InventoryType topInv = event.getView().getTopInventory().getType();
            if (topInv != InventoryType.PLAYER && topInv != InventoryType.CRAFTING) {
                ((Player) event.getWhoClicked()).spigot().sendMessage(C.m(getName(), "You cannot move the team inventory item out of your inventory"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(teamInventoryItems::contains);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (teamInventoryItems.contains(event.getItemDrop().getItemStack())) {
            event.getPlayer().spigot().sendMessage(C.m(getName(), "You cannot drop your team inventory item"));
            event.setCancelled(true);
        }
    }

    public void reset() {
        teamInventories.clear();
    }

    @Override
    public void run() {
        for(Player p : getPlugin().arena.players()){
            boolean fullInventory = true;
            for(int i = 0; i < p.getInventory().getSize(); i++){
                if(i == 36 || i == 37 || i == 38 || i == 39 || i == 40)
                    continue;
                if(p.getInventory().getItem(i) == null){
                    fullInventory = false;
                }
            }
            if(fullInventory){
                if(takeInventoryItem(p)){
                    p.sendMessage(UtilChat.message("Removing the team iventory item. Use "+ChatColor.GOLD+"/teaminv item"+ ChatColor.GRAY+" to get it back"));
                }
            }
        }
    }

    public boolean takeInventoryItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null)
                continue;
            if (teamInventoryItems.contains(item)) {
                player.getInventory().setItem(i, null);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void init() {
        registerListener(this);
        scheduleRepeating(this, 0, 5L);
    }
}
