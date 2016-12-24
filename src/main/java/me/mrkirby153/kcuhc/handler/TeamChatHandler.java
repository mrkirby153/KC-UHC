package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.team.LoneWolfTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.Module;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Objects;

public class TeamChatHandler extends Module<UHC> implements Listener {

    public TeamChatHandler(UHC uhc) {
        super("Team Chat", "1.0", uhc);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        UHCTeam team = getPlugin().teamHandler.getTeamForPlayer(event.getPlayer());
        if (team == null)
            return;
        if (event.getMessage().startsWith("@")) {
            event.setCancelled(true);
            if(team instanceof LoneWolfTeam){
                event.getPlayer().sendMessage(UtilChat.generateLegacyError("You cannot use team chat as a lone wolf!"));
                return;
            }
            String newMsg = event.getMessage().substring(1);
            team.getPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(player -> {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, SoundCategory.MASTER, 1.0F, 1.0F);
                player.sendMessage(ChatColor.BOLD + "TEAM " + team.getColor() + event.getPlayer().getName() + " " + ChatColor.WHITE + newMsg);
            });
        }
    }

    @Override
    protected void init() {
        registerListener(this);
    }
}
