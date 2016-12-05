package me.mrkirby153.kcuhc.handler;


import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.utils.UtilChat;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.NBTTagList;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RegenTicket implements Listener {

    private static HashMap<UUID, ItemStack> regenTickets = new HashMap<>();

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isTicket(event.getItemDrop().getItemStack()))
            event.setCancelled(true);
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

    @EventHandler
    public void inventoryMove(InventoryClickEvent event) {
        ItemStack is = event.getCurrentItem();
        if (isTicket(is)) {
            InventoryType topInv = event.getView().getTopInventory().getType();
            if (topInv != InventoryType.PLAYER && topInv != InventoryType.CRAFTING)
                event.setCancelled(true);
//                event.setCancelled(true);
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

    public static ItemStack createRegenTicket(Player player) {
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
        net.minecraft.server.v1_11_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag;
        if (!nmsStack.hasTag())
            tag = new NBTTagCompound();
        else
            tag = nmsStack.getTag();
        if (tag == null)
            tag = new NBTTagCompound();
        NBTTagList ench = nmsStack.getEnchantments() == null ? new NBTTagList() : null;
        UUID playerUUID = player.getUniqueId();
        tag.setString("owner", playerUUID.toString());
        tag.set("ench", ench);
        nmsStack.setTag(tag);
        ItemStack finalStack = CraftItemStack.asBukkitCopy(nmsStack);
        regenTickets.put(player.getUniqueId(), finalStack);
        return finalStack;
    }

    public static void give(Player player) {
        if (TeamHandler.isSpectator(player))
            return;
        player.spigot().sendMessage(UtilChat.generateFormattedChat("You have been given a regen ticket. This ticket will restore " +
                "you to full health. However, once PvP damage is given or delt, it is removed!", net.md_5.bungee.api.ChatColor.DARK_GREEN, 8));
        player.getInventory().addItem(createRegenTicket(player));
    }

    private boolean canUse(Player player, ItemStack stack) {
        net.minecraft.server.v1_11_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound tag = nmsStack.getTag();
        if (tag == null)
            return false;
        UUID u = UUID.fromString(tag.getString("owner"));
        return u.equals(player.getUniqueId());
    }

    public static void clearRegenTickets() {
        regenTickets.clear();
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
