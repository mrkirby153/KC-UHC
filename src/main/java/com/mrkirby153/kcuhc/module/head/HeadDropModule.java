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
        super("Drop Player Heads", "Player Heads will be dropped on death", Material.PLAYER_HEAD);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Location playerLoc = dead.getLocation();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(dead);
            head.setItemMeta(skullMeta);
        }
        playerLoc.getWorld().dropItemNaturally(playerLoc, head);
    }
}
