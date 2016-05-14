package me.mrkirby153.kcuhc.shop.item;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public interface Action {

    void onClick(Player player, ClickType clickType);

}
