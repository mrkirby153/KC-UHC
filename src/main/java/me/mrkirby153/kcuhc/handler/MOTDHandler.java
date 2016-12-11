package me.mrkirby153.kcuhc.handler;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcutils.Module;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class MOTDHandler extends Module<UHC>  implements Listener {

    private static String motd = "";
    private static final String MOTD_HEADER = ChatColor.RED + "" + ChatColor.BOLD + "Kirbycraft plugin: \n";


    public static void setMotd(String newMotd) {
        motd = newMotd;
    }

    private UHC plugin;

    public MOTDHandler(UHC plugin) {
        super("MOTD Manager", "1.0", plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void serverPingEvent(ServerListPingEvent event) {
        event.setMotd(MOTD_HEADER + motd);
    }

    @EventHandler
    public void playerJoin(final PlayerLoginEvent event) {
        if (plugin.arena != null) {
            if (plugin.arena.currentState() == UHCArena.State.GENERATING_WORLD) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "We're not quite ready for you yet\n" + ChatColor.GOLD + "Check back in a few");
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if(plugin.arena.currentState() != UHCArena.State.RUNNING && plugin.arena.currentState() != UHCArena.State.FROZEN) {
                        if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(plugin.arena.getCenter().getWorld().getName()))
                            event.getPlayer().teleport(plugin.arena.getCenter().add(0, 2, 0));
                        else if (event.getPlayer().getLocation().distanceSquared(plugin.arena.getCenter()) > Math.pow(50, 2))
                            event.getPlayer().teleport(plugin.arena.getCenter().add(0, 2, 0));
                        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
                        BaseComponent line = UtilChat.generateFormattedChat("=============================================", ChatColor.GREEN, 10);
                        BaseComponent padding = new TextComponent(" ");

                        BaseComponent greeting = UtilChat.generateFormattedChat("Welcome to KC-plugin ", ChatColor.WHITE, 8);
                        greeting.addExtra(UtilChat.generateFormattedChat(event.getPlayer().getName(), ChatColor.GOLD, 8));
                        UtilChat.sendMultiple(event.getPlayer(), line, greeting);

                        BaseComponent notStarted = UtilChat.generateFormattedChat("   The game hasn't started yet so hold tight!", ChatColor.WHITE, 0);
                        if(plugin.getConfig().getBoolean("discord.useDiscord")){
                            event.getPlayer().spigot().sendMessage(notStarted);
                            event.getPlayer().spigot().sendMessage(padding);
                            BaseComponent usingDiscord = UtilChat.generateFormattedChat("   This server is using ", ChatColor.WHITE, 0);
                            usingDiscord.addExtra(UtilChat.generateFormattedChat("Discord", ChatColor.BLUE, 0));
                            usingDiscord.addExtra(UtilChat.generateFormattedChat(" to manage team chat.", ChatColor.WHITE, 0));
                            event.getPlayer().spigot().sendMessage(usingDiscord);

                            BaseComponent discordInvite = UtilChat.generateFormattedChat("   Join the discord server by clicking ", ChatColor.WHITE, 0);
                            discordInvite.addExtra(UtilChat.generateHyperlink(UtilChat.generateFormattedChat("[HERE]", ChatColor.BLUE, 0), plugin.getConfig().getString("discord.inviteUrl")));
                            discordInvite.addExtra(UtilChat.generateHyperlink(UtilChat.generateFormattedChat(" [MORE INFO]", ChatColor.BLUE), "https://docs.google.com/document/d/1nbrxbNHjko88v_CHoXLbfvZzC2b81hojC8j-OfOU0lQ/edit?usp=sharing"));
                            BaseComponent linkCommand = UtilChat.generateFormattedChat("   Once you have joined the discord server, type ", ChatColor.WHITE, 0);
                            TextComponent cmd = new TextComponent("/discord link");
                            cmd.setColor(ChatColor.AQUA);
                            cmd.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Click here to get your discord link code!")}));
                            cmd.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/discord link"));
                            linkCommand.addExtra(cmd);
                            BaseComponent linkCmdCont = UtilChat.generateFormattedChat("   to generate your link code!", ChatColor.WHITE, 0);

                            UtilChat.sendMultiple(event.getPlayer(), discordInvite, padding, linkCommand, linkCmdCont);
                        } else {
                            UtilChat.sendMultiple(event.getPlayer(), padding, padding, padding, notStarted, padding, padding, padding);
                        }
                        event.getPlayer().spigot().sendMessage(line);
                    }
                }
            }.runTaskLater(plugin, 10L);
        }
    }

    @Override
    protected void init() {
        registerListener(this);
    }
}
