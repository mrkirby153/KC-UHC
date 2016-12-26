package me.mrkirby153.kcuhc.module.msc;

import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.utils.UtilChat;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RegenTicketModule extends UHCModule {

    private HashMap<UUID, ItemStack> regenTickets = new HashMap<>();
    private TeamHandler teamHandler;

    public RegenTicketModule(TeamHandler teamHandler) {
        super(Material.PAPER, 0, "Regen Tickets", true, "Gives everyone a regen ticket");
        this.teamHandler = teamHandler;
    }

    public void clearRegenTickets() {
        regenTickets.clear();
    }

    public ItemStack createRegenTicket(Player player) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Regen Ticket" + ChatColor.GREEN + " (Right Click)");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.RESET + "The regen ticket is a one-time");
        lore.add(ChatColor.RESET + "ticket that will restore you to");
        lore.add(ChatColor.RESET + "full health.");
        lore.add("");
        lore.add(ChatColor.RED + "This ticket becomes void if PvP combat");
        lore.add(ChatColor.RED + "damage is given or taken");
        lore.add("");
        lore.add(ChatColor.RED + "This ticket is non-transferable and");
        lore.add(ChatColor.RED + "can only be used by " + ChatColor.GOLD + player.getName());
        meta.setLore(lore);
        item.setItemMeta(meta);
        regenTickets.put(player.getUniqueId(), item);
        return item;
    }

    public void give(Player player) {
        if (teamHandler.isSpectator(player))
            return;
        player.spigot().sendMessage(UtilChat.generateFormattedChat("You have been given a regen ticket. This ticket will restore " +
                "you to full health. However, once PvP damage is given or delt, it is removed!", net.md_5.bungee.api.ChatColor.DARK_GREEN, 8));
        player.getInventory().addItem(createRegenTicket(player));
    }


    @EventHandler
    public void inventoryMove(InventoryClickEvent event) {
        ItemStack is = event.getCurrentItem();
        if (isTicket(is) && canUse((Player) event.getWhoClicked(), is)) {
            InventoryType topInv = event.getView().getTopInventory().getType();
            if (topInv != InventoryType.PLAYER && topInv != InventoryType.CRAFTING) {
                event.getWhoClicked().sendMessage(UtilChat.generateLegacyError("You cannot move your regen ticket out of your inventory"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if(isTicket(event.getItem().getItemStack())){
            if(!canUse(event.getPlayer(), event.getItem().getItemStack()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack stack = event.getItem();
            if (isTicket(stack)) {
                if (!canUse(event.getPlayer(), stack)) {
                    event.getPlayer().spigot().sendMessage(UtilChat.generateError("You cannot use someone else's regen ticket!"));
                    return;
                }
                Player clicker = event.getPlayer();
                if (clicker.getHealth() >= clicker.getMaxHealth()) {
                    event.getPlayer().spigot().sendMessage(UtilChat.generateError("You cannot use this while you have full health!"));
                    return;
                }
                double target = clicker.getMaxHealth() - 4;
                if (clicker.getHealth() >= target) {
                    event.getPlayer().spigot().sendMessage(UtilChat.generateError("You cannot use this while you have more than " + target / 2 + " hearts!"));
                    return;
                }
                remove(clicker, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled())
            return;
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return; // Not involving a player
        }
        // Check if the player hit a player
        if (event.getEntity() instanceof Player) {
            // They hit a player, remove the item
            remove((Player) event.getDamager(), false);
        }
        // Check if the player is hit by a player
        if (event.getDamager() instanceof Player) {
            // They were hit by a player, remove the item
            remove((Player) event.getEntity(), false);
        }
    }

    @Override
    public void onDisable() {
        regenTickets.forEach(((uuid, itemStack) -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                remove(p, false);
        }));
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isTicket(event.getItemDrop().getItemStack())) {
            if(canUse(event.getPlayer(), event.getItemDrop().getItemStack())) {
                event.getPlayer().sendMessage(UtilChat.message("You cannot drop your regen ticket"));
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onEnable() {

    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == UHCArena.State.RUNNING) {
            for (Player p : getPlugin().arena.players()) {
                give(p);
            }
        }
        if(event.getTo() == UHCArena.State.ENDGAME)
            clearRegenTickets();
    }

    private boolean canUse(Player player, ItemStack stack) {
        ItemStack itemStack = regenTickets.get(player.getUniqueId());
        return itemStack != null && itemStack.equals(stack);
    }

    private boolean isTicket(ItemStack stack) {
        return regenTickets.containsValue(stack);
    }

    private void remove(Player player, boolean heal) {
        ItemStack regenTicket = regenTickets.remove(player.getUniqueId());
        if (regenTicket == null)
            return;
        PlayerInventory inventory = player.getInventory();
        inventory.remove(regenTicket);
        if (!heal) {
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_DOOR_WOOD, 1, 1);
            player.spigot().sendMessage(UtilChat.generateFormattedChat("Your regen ticket has been removed!", net.md_5.bungee.api.ChatColor.RED, 0));
        } else {
            double currHealth = player.getHealth();
            double maxHealth = player.getMaxHealth();
            double healAmount = maxHealth - currHealth;
            int healed = 20 - player.getFoodLevel();
            player.setHealth(maxHealth);
            player.setFoodLevel(20);
            player.setExhaustion(3.8F);
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1, 1);
            player.sendMessage(UtilChat.message("You have restored " + ChatColor.GOLD + (int) healAmount + ChatColor.GRAY + " health and " + ChatColor.GOLD + healed + ChatColor.GRAY + " hunger"));
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 300);
        }
    }
}
