package com.mrkirby153.kcuhc.module.head;

import com.mrkirby153.kcuhc.module.UHCModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class HeadDropModule extends UHCModule {

    public HeadDropModule() {
        super("Drop Player Heads", "Player Heads will be dropped on death", Material.SKULL_ITEM, 3);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Location playerLoc = dead.getLocation();
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwner(dead.getName());
        head.setItemMeta(skullMeta);
        playerLoc.getWorld().dropItemNaturally(playerLoc, head);
    }
}
