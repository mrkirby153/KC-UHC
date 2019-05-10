package com.mrkirby153.kcuhc.module.respawner;

import me.mrkirby153.kcutils.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;
import javax.annotation.Nullable;

public class SoulVialHandler {

    private static SoulVialHandler instance;

    private HashMap<ItemStack, UUID> soulVials = new HashMap<>();

    // Private constructor
    private SoulVialHandler() {

    }

    public static SoulVialHandler getInstance() {
        if (instance == null) {
            instance = new SoulVialHandler();
        }
        return instance;
    }

    public ItemStack getSoulVial(Player player) {
        ItemStack stack = new ItemFactory(Material.EXPERIENCE_BOTTLE)
            .name(ChatColor.GREEN + "Soul Vial: " + ChatColor.YELLOW + "(" + player.getName() + ")")
            .lore("Place this in a teammate respawner to revive this teammate").construct();
        this.soulVials.put(stack, player.getUniqueId());
        return stack;
    }

    public boolean isSoulVial(ItemStack itemStack) {
        return this.soulVials.keySet().contains(itemStack);
    }

    public void clearSoulVials() {
        this.soulVials.clear();
    }

    public void useSoulVial(ItemStack stack) {
        this.soulVials.remove(stack);
    }

    @Nullable
    public Player getSoulVialContents(ItemStack itemStack) {
        UUID u = this.soulVials.get(itemStack);
        if (u == null) {
            return null;
        }
        return Bukkit.getPlayer(u);
    }
}
