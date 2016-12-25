package me.mrkirby153.kcuhc.module.head;

import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.utils.UtilChat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class DropPlayerHeadModule extends UHCModule {

    public DropPlayerHeadModule() {
        super(Material.SKULL_ITEM, 3, "Drop Player Heads", true, "Players drop heads on death");
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("Dropping player heads on death!"));
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("No longer dropping player heads on death!"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Location playerLoc = dead.getLocation();
        // Drop the player's head
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta m = head.getItemMeta();
        ((SkullMeta) m).setOwner(dead.getName());
        head.setItemMeta(m);
        playerLoc.getWorld().dropItemNaturally(playerLoc, head);
    }
}
