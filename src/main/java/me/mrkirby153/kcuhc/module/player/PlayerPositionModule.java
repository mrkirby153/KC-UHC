package me.mrkirby153.kcuhc.module.player;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import me.mrkirby153.kcutils.nms.NMS;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class PlayerPositionModule extends UHCModule {

    private NMS nms;
    private TeamHandler teamHandler;

    private boolean running = false;

    public PlayerPositionModule(NMS nms, TeamHandler teamHandler) {
        super(Material.BEACON, 0, "Player Position", true, "Display player position in the action bar when the game is running");
        this.nms = nms;
        this.teamHandler = teamHandler;
    }

    @Override
    public void onEnable() {
        running = UHC.getInstance().arena.currentState() == UHCArena.State.RUNNING;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        this.running = event.getTo() == UHCArena.State.RUNNING;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.TICK)
            return;
        if(!running)
            return;
        for(Player p : Bukkit.getOnlinePlayers()){
            if(teamHandler.isSpectator(p))
                continue;
            TextComponent bc;
            Location l = p.getLocation();
            bc = (TextComponent) UtilChat.generateFormattedChat("Current Position: ", ChatColor.GOLD, 0);
            bc.addExtra(UtilChat.generateFormattedChat("X: ", ChatColor.RED, 0));
            bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getX()), ChatColor.GREEN, 0));
            bc.addExtra(UtilChat.generateFormattedChat(" Y: ", ChatColor.RED, 0));
            bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getY()), ChatColor.GREEN, 0));
            bc.addExtra(UtilChat.generateFormattedChat(" Z: ", ChatColor.RED, 0));
            bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getZ()), ChatColor.GREEN, 0));
            nms.actionBar(p, bc);
        }
    }
}
