package com.mrkirby153.kcuhc.player;

import com.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ActionBarManager implements Listener {

    private static ActionBarManager INSTANCE;
    private final List<ActionBar> registeredActionBars = new CopyOnWriteArrayList<>();
    private UHC uhc;

    private ActionBarManager(UHC uhc) {
        // Dummy constructor to enforce singleton pattern
        this.uhc = uhc;
        uhc.getServer().getPluginManager().registerEvents(this, uhc);
    }

    public static ActionBarManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ActionBarManager has not been initialized");
        }
        return INSTANCE;
    }

    public static void init(UHC uhc) {
        INSTANCE = new ActionBarManager(uhc);
    }

    /**
     * Updates the action bars for all players
     */
    public void updateActionBars() {
        Bukkit.getOnlinePlayers().forEach(this::updateActionBar);
    }

    /**
     * Updates the action bar for a player
     *
     * @param player The player
     */
    public void updateActionBar(Player player) {
        ActionBar toSend = getBarToSend(getAllBars(player));
        if (toSend == null) {
            return;
        }
        BaseComponent tc = toSend.get(player);
        uhc.protocolLibManager.sendActionBar(player, tc);
    }

    /**
     * Registers an action bar
     *
     * @param bar The bar to register
     */
    public void registerActionBar(ActionBar bar) {
        registeredActionBars.add(bar);
    }

    /**
     * Unregisters an action bar
     *
     * @param bar The bar to unregister
     */
    public void unregisterActionBar(ActionBar bar) {
        registeredActionBars.remove(bar);
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == UpdateType.FAST) {
            updateActionBars();
        }
    }

    private List<ActionBar> getAllBars(Player player) {
        return registeredActionBars.stream().filter(bar -> bar.get(player) != null).collect(
            Collectors.toList());
    }

    /**
     * Gets the bar that should be sent to the player
     *
     * @param bars The list of bars
     */
    private ActionBar getBarToSend(List<ActionBar> bars) {
        if (bars.size() < 1) {
            return null;
        }
        if (bars.size() == 1) {
            return bars.get(0);
        }
        ActionBar toSend = bars.get(0);
        for (ActionBar b : bars) {
            if (b.getPriority() > toSend.getPriority()) {
                toSend = b;
            }
        }
        return toSend;
    }
}
