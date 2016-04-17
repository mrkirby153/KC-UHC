package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.UtilChat;
import me.mrkirby153.kcuhc.arena.UHCArena;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;
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
            if (UHC.arena.currentState() != UHCArena.State.RUNNING)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(UHC.arena.getCenter().getWorld().getName()))
                            event.getPlayer().teleport(UHC.arena.getCenter().add(0, 2, 0));
                        else if (event.getPlayer().getLocation().distanceSquared(UHC.arena.getCenter()) > Math.pow(50, 2))
                            event.getPlayer().teleport(UHC.arena.getCenter().add(0, 2, 0));
                        if (UHC.handler != null) {
                            if (!UHC.handler.hasLinkedDiscord(event.getPlayer().getUniqueId())) {
                                BaseComponent greeting = UtilChat.generateFormattedChat("Hello, ", ChatColor.GOLD, 0);
                                greeting.addExtra(UtilChat.generateFormattedChat(event.getPlayer().getName(), ChatColor.GREEN, 0));
                                greeting.addExtra(UtilChat.generateFormattedChat(" and welcome to TKA UHC", ChatColor.GOLD, 0));

                                BaseComponent discordNotification = UtilChat.generateFormattedChat("It looks like you haven't linked your minecraft account to Discord yet.", ChatColor.GOLD, 0);

                                BaseComponent joinServer = UtilChat.generateFormattedChat("Please join the server by clicking ", ChatColor.GREEN, 0);
                                joinServer.addExtra(UtilChat.generateHyperlink(UtilChat.generateFormattedChat("[HERE]", ChatColor.BLUE, 0), UHC.plugin.getConfig().getString("discord.inviteUrl"),
                                        UtilChat.generateFormattedChat("Join the discord server", ChatColor.DARK_PURPLE, 0)));

                                BaseComponent browserWarning = UtilChat.generateBoldChat("Please note that voice chat is only compatible with Firefox, Chrome, and Opera", ChatColor.DARK_RED);

                                BaseComponent command = UtilChat.generateFormattedChat("Once you've joined the server, type ", ChatColor.GOLD, 0);
                                command.addExtra(UtilChat.generateFormattedChat("/discord link", ChatColor.BLUE, 0));
                                command.addExtra(UtilChat.generateFormattedChat(" to get your unique link code", ChatColor.GOLD, 0));

                                Player.Spigot s = event.getPlayer().spigot();
                                s.sendMessage(greeting);
                                s.sendMessage(discordNotification);
                                s.sendMessage(joinServer);
                                s.sendMessage(browserWarning);
                                s.sendMessage(command);
                            }
                        }
                    }
                }.runTaskLater(UHC.plugin, 10L);
        }
    }
}
