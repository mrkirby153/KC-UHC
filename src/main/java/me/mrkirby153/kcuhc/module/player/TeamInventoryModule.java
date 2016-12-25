package me.mrkirby153.kcuhc.module.player;

import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.team.LoneWolfTeam;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.C;
import me.mrkirby153.kcutils.ItemFactory;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TeamInventoryModule extends UHCModule {

    private TeamHandler teamHandler;
    private HashMap<UHCTeam, Inventory> teamInventories = new HashMap<>();
    private Set<ItemStack> teamInventoryItems = new HashSet<>();

    public TeamInventoryModule(TeamHandler teamHandler) {
        super(Material.ENDER_CHEST, 0, "Team Inventories", false, "A shared inventory between team members");
        this.teamHandler = teamHandler;
    }

    public Inventory getInventory(Player player) {
        if (teamHandler.getTeamForPlayer(player) != null)
            return getInventory(teamHandler.getTeamForPlayer(player));
        return null;
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
            teamInventories.put(team, inv = Bukkit.createInventory(null, 9 * 2, "Team Inventory: " + name));
        }
        return inv;
    }

    public void giveInventoryItem(Player player) {
        UHCTeam team = teamHandler.getTeamForPlayer(player);
        if (team == null)
            return;
        if (!spaceInInvenotry(player)) {
            player.spigot().sendMessage(C.m(getName(), "There is no space in your inventory!"));
            return;
        }
        ItemStack inventory = new ItemFactory(Material.ENDER_CHEST).name(ChatColor.WHITE + "Team Inventory: " + team.getColor() + team.getFriendlyName())
                .lore("Right click to open your team's inventory").construct();
        this.teamInventoryItems.add(inventory);
        player.getInventory().addItem(inventory);
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

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("Team inventories disabled!"));
        Bukkit.getOnlinePlayers().forEach(this::takeInventoryItem);
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("Team inventories enabled!"));
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
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == UHCArena.State.RUNNING) {
            for (Player p : getPlugin().arena.players()){
                if(teamHandler.getTeamForPlayer(p) instanceof LoneWolfTeam)
                    continue;
                giveInventoryItem(p);
            }
        }
        if(event.getTo() == UHCArena.State.ENDGAME){
            this.teamInventories.clear();
            teamInventoryItems.clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryClickEvent event) {
        if (teamInventoryItems.contains(event.getCurrentItem())) {
            if (event.getClick() == ClickType.RIGHT) {
                event.setCancelled(true);
                event.getWhoClicked().closeInventory();
                event.getWhoClicked().openInventory(getInventory((Player) event.getWhoClicked()));
                return;
            }
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

    private boolean spaceInInvenotry(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            // Skip offhand and armour slots
            if (i == 36 || i == 37 || i == 38 || i == 39 || i == 40)
                continue;
            if (player.getInventory().getItem(i) == null)
                return true;
        }
        return false;
    }
}
