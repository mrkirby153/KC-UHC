package com.mrkirby153.kcuhc.module.msc;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.GameState;
import com.mrkirby153.kcuhc.game.event.GameStateChangeEvent;
import com.mrkirby153.kcuhc.game.team.SpectatorTeam;
import com.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.Chat;
import me.mrkirby153.kcutils.Chat.Style;
import me.mrkirby153.kcutils.scoreboard.ScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
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
    private UHC uhc;

    @Inject
    public RegenTicketModule(UHC uhc) {
        super("Regen Ticket", "Gives everyone a regen ticket", Material.PAPER);
        this.uhc = uhc;
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
        if (isSpectator(player)) {
            return;
        }
        player.spigot()
            .sendMessage(Chat.formattedChat("You have been given a regen ticket."
                    + " This ticket will restore you to full health. However, once PvP damage is given or delt, it is removed",
                net.md_5.bungee.api.ChatColor.GREEN, Style.BOLD));
        player.getInventory().addItem(createRegenTicket(player));
    }


    @EventHandler
    public void inventoryMove(InventoryClickEvent event) {
        ItemStack is = event.getCurrentItem();
        if (isTicket(is) && canUse((Player) event.getWhoClicked(), is)) {
            InventoryType topInv = event.getView().getTopInventory().getType();
            if (topInv != InventoryType.PLAYER && topInv != InventoryType.CRAFTING) {
                event.getWhoClicked().sendMessage(
                    Chat.error("You cannot move your regen ticket out of your inventory")
                        .toLegacyText());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR
            || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack stack = event.getItem();
            if (isTicket(stack)) {
                if (!canUse(event.getPlayer(), stack)) {
                    event.getPlayer().spigot().sendMessage(
                        Chat.error("You cannot use someone else's regen ticket!"));
                    return;
                }
                Player clicker = event.getPlayer();
                if (clicker.getHealth() >= clicker.getAttribute(Attribute.GENERIC_MAX_HEALTH)
                    .getValue()) {
                    event.getPlayer().spigot().sendMessage(
                        Chat.error("You cannot use this while you have full health!"));
                    return;
                }
                double target = clicker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - 4;
                if (clicker.getHealth() >= target) {
                    if (!clicker.isSneaking()) {
                        event.getPlayer().spigot().sendMessage(Chat.message("Error",
                            "It is advised to wait until you have less than {hearts} hearts before using this."
                                +
                                " If you wish to do so anyways, sneak and try again",
                            "{hearts}", target / 2));
                        return;
                    } else {
                        remove(clicker, true);
                    }
                }
                remove(clicker, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
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

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isTicket(event.getItemDrop().getItemStack())) {
            if (canUse(event.getPlayer(), event.getItemDrop().getItemStack())) {
                event.getPlayer().sendMessage(
                    Chat.error("You cannot drop your regen ticket").toLegacyText());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == GameState.ALIVE) {
            Bukkit.getOnlinePlayers().forEach(this::give);
        }
        if (event.getTo() == GameState.ENDING) {
            clearRegenTickets();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (isTicket(event.getItem().getItemStack())) {
            if (!canUse(event.getPlayer(), event.getItem().getItemStack())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean canUse(Player player, ItemStack stack) {
        ItemStack itemStack = regenTickets.get(player.getUniqueId());
        return itemStack != null && itemStack.equals(stack);
    }

    private boolean isSpectator(Player player) {
        ScoreboardTeam team = this.uhc.getGame().getTeam(player);
        return team != null && (team instanceof SpectatorTeam);
    }

    private boolean isTicket(ItemStack stack) {
        return regenTickets.containsValue(stack);
    }

    private void remove(Player player, boolean heal) {
        ItemStack regenTicket = regenTickets.remove(player.getUniqueId());
        if (regenTicket == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        inventory.remove(regenTicket);
        if (!heal) {
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_DOOR_WOOD, 1, 1);
            player.spigot().sendMessage(Chat
                .formattedChat("Your regen ticket has been removed!",
                    net.md_5.bungee.api.ChatColor.RED, Chat.Style.BOLD));
        } else {
            double currHealth = player.getHealth();
            double maxHealth = player.getMaxHealth();
            double healAmount = maxHealth - currHealth;
            int healed = 20 - player.getFoodLevel();
            player.setHealth(maxHealth);
            player.setFoodLevel(20);
            player.setExhaustion(3.8F);
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1, 1);
            player.sendMessage(
                Chat.message("", "You have restored {health} health and {hunger} hunger",
                    "{health}", (int) healAmount, "{hunger}", healed).toLegacyText());
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 300);
        }
    }
}
