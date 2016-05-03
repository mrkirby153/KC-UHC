package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.arena.UHCArena;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class MOTDHandler implements Listener {

    private static String motd = "";
    private static final String MOTD_HEADER = ChatColor.RED + "" + ChatColor.BOLD + "Kirbycraft UHC: \n";


    public static void setMotd(String newMotd) {
        motd = newMotd;
    }

    @EventHandler
    public void serverPingEvent(ServerListPingEvent event) {
        event.setMotd(MOTD_HEADER + motd);
    }

    @EventHandler
    public void playerJoin(final PlayerLoginEvent event) {
        if (UHC.arena != null) {
            if (UHC.arena.currentState() == UHCArena.State.GENERATING_WORLD) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "We're not quite ready for you yet\n" + ChatColor.GOLD + "Check back in a few");
            }
            if (UHC.arena.currentState() != UHCArena.State.RUNNING && UHC.plugin.getConfig().getBoolean("discord.useDiscord"))
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(UHC.arena.getCenter().getWorld().getName()))
                            event.getPlayer().teleport(UHC.arena.getCenter().add(0, 2, 0));
                        else if (event.getPlayer().getLocation().distanceSquared(UHC.arena.getCenter()) > Math.pow(50, 2))
                            event.getPlayer().teleport(UHC.arena.getCenter().add(0, 2, 0));
                        BaseComponent greeting = UtilChat.generateFormattedChat("Welcome to KC-UHC ", ChatColor.YELLOW);
                        greeting.addExtra(UtilChat.generateFormattedChat(event.getPlayer().getName(), ChatColor.GREEN));

                        BaseComponent discord = UtilChat.generateFormattedChat("If you have not done so already, please join the discord server by clicking ", ChatColor.YELLOW);
                        discord.addExtra(UtilChat.generateHyperlink(UtilChat.generateFormattedChat("[HERE]", ChatColor.BLUE, 8), UHC.plugin.getConfig().getString("discord.inviteUrl"),
                                UtilChat.generateFormattedChat("Join the discord server", ChatColor.WHITE)));

                        BaseComponent browserWarning = UtilChat.generateBoldChat("Please note that voice chat is only supported on the offical discord client, Firefox, Chrome, and Opera", ChatColor.DARK_RED);

                        BaseComponent linkCommand = UtilChat.generateFormattedChat("Once you have joined the discord server, click ", ChatColor.YELLOW);
                        TextComponent cmd = new TextComponent("[HERE]");
                        cmd.setColor(ChatColor.BLUE);
                        cmd.setBold(true);
                        cmd.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{UtilChat.generateFormattedChat("Click to get your link code!", ChatColor.WHITE)}));
                        cmd.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/discord link"));
                        linkCommand.addExtra(cmd);
                        linkCommand.addExtra(UtilChat.generateFormattedChat(" to link your account with discord!", ChatColor.YELLOW));

                        UtilChat.sendMultiple(event.getPlayer(), greeting, discord, browserWarning, linkCommand);
                    }
                }.runTaskLater(UHC.plugin, 10L);
        }
    }
}
