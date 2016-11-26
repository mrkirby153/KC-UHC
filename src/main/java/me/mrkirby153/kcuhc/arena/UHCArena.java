package me.mrkirby153.kcuhc.arena;


import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.handler.EndgameHandler;
import me.mrkirby153.kcuhc.arena.handler.TeamInventoryHandler;
import me.mrkirby153.kcuhc.gui.SpecInventory;
import me.mrkirby153.kcuhc.handler.*;
import me.mrkirby153.kcuhc.scoreboard.UHCScoreboard;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.TeamSpectator;
import me.mrkirby153.kcuhc.team.UHCPlayerTeam;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTime;
import me.mrkirby153.kcuhc.utils.UtilTitle;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandAssignSpectator;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandToLobby;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandAssignTeams;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandNewTeam;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandRemoveTeam;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_10_R1.IChatBaseComponent;
import net.minecraft.server.v1_10_R1.PacketPlayOutChat;
import net.minecraft.server.v1_10_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_10_R1.PacketPlayOutNamedEntitySpawn;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static me.mrkirby153.kcuhc.UHC.plugin;
import static me.mrkirby153.kcuhc.UHC.uhcNetwork;
import static me.mrkirby153.kcuhc.arena.UHCArena.State.*;
import static me.mrkirby153.kcuhc.utils.UtilChat.generateBoldChat;

public class UHCArena implements Runnable, Listener {


    private static final int WORLDBORDER_WARN_DIST = 50;
    private static final int TIP_TIME = 30000;
    // Worlds

    public ScoreboardUpdater scoreboardUpdater;
    protected String winner;
    protected long startTime = 0;
    CountdownBarTask countdownTask;
    private EndgameHandler endgameHandler;
    private ArenaProperties properties;
    private boolean firstAnnounce = true;
    private boolean shouldAnnounce = false;
    private String lastAnnounced = "-1";

    private ArrayList<Player> players = new ArrayList<>();

    private State state = State.INITIALIZED;

    private int generationTaskId;
    private int countdown;

    private WorldBorderHandler worldBorderHandler;

    private int launchedFw = 0;
    private Color winningTeamColor = Color.WHITE;

    private List<Player> previouslyOpped = new ArrayList<>();

    private HashMap<UUID, String> uuidToStringMap = new HashMap<>();
    private HashMap<UUID, Long> logoutTimes = new HashMap<>();
    private ArrayList<UUID> queuedTeamRemovals = new ArrayList<>();

    private int secondsRemaining;
    private double overworldWorldborderSize;
    private double netherWorldborderSize;
    private long freezeStartTime;

    private boolean notifiedDisabledSpawn = false;

    private String[] tips = new String[]{
            ChatColor.GOLD + "Craft a Head Apple by surrounding a player head in gold!",
            ChatColor.YELLOW + "Use a player tracker (Compass) to find enemies!",
            ChatColor.GOLD + "5 Minutes after the worldborder stops, everyone gets Hunger III",
            ChatColor.YELLOW + "Watch out for the worldborder! It can sneak up on you quickly!",
            ChatColor.GOLD + "%NAME% is going to win!",
            ChatColor.YELLOW + "This tip message format was NOT stolen from Mineplex",
            ChatColor.GOLD + "Message'); DROP TABLE `tips`; --",
            ChatColor.GOLD + "It may or may not be a good idea to not die",
            ChatColor.YELLOW + "This is not the tip you are looking for",
            ChatColor.BLUE + "Hey! Isn't this tip supposed to be yellow or gold?",
            ChatColor.GOLD + "Was I supposed to write legitimate tips? Oops.",
            ChatColor.YELLOW + "You mean whatever I write here will show up as a tip? HI MOM",
            ChatColor.GOLD + "In the event of an emergency, do not panic! Use /a <message> to contact an admin!",
            ChatColor.YELLOW + "Access your team-specific inventory with " + ChatColor.GREEN + "/teaminv"

    };

    private long nextTipIn = -1;
    private int runCount = 0;

    private long graceUntil = -1;

    private TeamInventoryHandler teamInventoryHandler;


    public UHCArena(String presetFile) {
        if (presetFile == null)
            presetFile = "default";
        this.properties = ArenaProperties.loadProperties(presetFile);

        this.worldBorderHandler = new WorldBorderHandler(UHC.plugin, this);
        this.scoreboardUpdater = new ScoreboardUpdater(this, new UHCScoreboard());
        this.endgameHandler = new EndgameHandler(this);
        this.teamInventoryHandler = new TeamInventoryHandler();

        UHC.plugin.getServer().getPluginManager().registerEvents(new PregameListener(), UHC.plugin);
        UHC.plugin.getServer().getPluginManager().registerEvents(new GameListener(), UHC.plugin);
        UHC.plugin.getServer().getPluginManager().registerEvents(this, UHC.plugin);

        UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, this, 0, 20);
        UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, scoreboardUpdater::refresh, 0, 1L);
        UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, endgameHandler, 0, 1);

        craftingRecipes();
    }

    public UHCArena() {
        this("default");
    }

    public void addPlayer(Player player) {
        Iterator<Player> p = players.iterator();
        while (p.hasNext()) {
            if (p.next().getUniqueId().equals(player.getUniqueId()))
                p.remove(); // Remove old player object
        }
        this.players.add(player);
    }

    public void announcePvPGrace() {
        double msRemaining = this.graceUntil - System.currentTimeMillis();
        double secondsRemaining = Math.floor(msRemaining / 1000D);
        double minutesRemaining = Math.floor(secondsRemaining / 60D);
        if (secondsRemaining <= 0) {
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1F, 1F));
            Bukkit.broadcastMessage(UtilChat.message(ChatColor.RED + "" + ChatColor.BOLD + "PVP ENABLED!"));
            return;
        }
        if (minutesRemaining >= 1) {
            if (secondsRemaining % 60 == 0) {
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F));
                Bukkit.broadcastMessage(UtilChat.message(ChatColor.GREEN + "PVP will be enabled in " + UtilTime.format(1, (long) msRemaining, UtilTime.TimeUnit.FIT)));
            }
        } else {
            if (secondsRemaining < 10 || secondsRemaining == 30 || secondsRemaining == 15) {
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F));
                Bukkit.broadcastMessage(UtilChat.message(ChatColor.GREEN + "" + ChatColor.BOLD + "PVP will be enabled in " + secondsRemaining + " seconds"));
            }
        }
    }

    public void bringEveryoneToLobby() {
        if (UHC.uhcNetwork != null) {
            new BotCommandToLobby(UHC.plugin.serverId()).publish();
            TeamHandler.teams().forEach(t -> new BotCommandRemoveTeam(UHC.plugin.serverId(), t.getName()).publish());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void chatEvent(AsyncPlayerChatEvent event) {
        UHCTeam team = TeamHandler.getTeamForPlayer(event.getPlayer());
        if (team != null) {
            if (team instanceof TeamSpectator) {
                event.setFormat(ChatColor.GRAY + "Spectator %s " + ChatColor.WHITE + "%s");
            } else {
                event.setFormat(team.getColor() + "%s " + ChatColor.WHITE + "%s");
            }
        } else {
            event.setFormat(ChatColor.GRAY + "%s " + ChatColor.WHITE + "%s");
        }
    }

    @EventHandler
    public void consumeHeadApple(PlayerItemConsumeEvent event) {
        if (event.getItem().getItemMeta().getDisplayName() == null)
            return;
        if (!event.getItem().getItemMeta().getDisplayName().contains("Head"))
            return;
        if (!event.getItem().getEnchantments().containsKey(Enchantment.ARROW_DAMAGE))
            return;
        if (event.getItem().getType() != Material.GOLDEN_APPLE) {
            return;
        }

        event.getPlayer().sendMessage(UtilChat.message("You ate a head apple"));

        UHCTeam team = TeamHandler.getTeamForPlayer(event.getPlayer());
        if (team != null) {
            team.getPlayers().stream().map(Bukkit::getPlayer).filter(p -> p != null).forEach(p -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));

                if (p.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                    p.sendMessage(UtilChat.message("You have given your team Regeneration II and Absorption!"));
                } else {
                    p.sendMessage(UtilChat.message(ChatColor.GOLD + event.getPlayer().getName() + ChatColor.GRAY + " ate a head apple, giving you Regeneration II and Absorption!"));
                }
            });
        } else {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));
            event.getPlayer().sendMessage(UtilChat.message("You are not on a team so only you get the effects"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void craftGoldenHead(PrepareItemCraftEvent event) {
        if (event.getRecipe().getResult() == null)
            return;
        Material type = event.getRecipe().getResult().getType();
        if (type != Material.GOLDEN_APPLE)
            return;
        if (event.getInventory() == null)
            return;

        if (properties.ENABLE_HEAD_APPLE.get()) {
            CraftingInventory inv = event.getInventory();
            for (ItemStack item : inv.getMatrix()) {
                if (item != null && item.getType() != Material.AIR) {
                    if (item.getType() == Material.SKULL_ITEM || item.getType() == Material.SKULL) {
                        if (item.getItemMeta() == null)
                            continue;
                        ItemStack apple = new ItemStack(Material.GOLDEN_APPLE, 1);
                        ItemMeta meta = apple.getItemMeta();
                        meta.setDisplayName(ChatColor.AQUA + "Head Apple");
                        apple.setItemMeta(meta);
                        apple.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
                        inv.setResult(apple);
                        return;
                    }
                }
            }
        } else {
            event.getInventory().setResult(null);
        }
    }

    public State currentState() {
        return this.state;
    }

    public void distributeTeams(int minRadius) {
        System.out.println("Worldborder location: +-" + getWorld().getWorldBorder().getSize() / 2);
        System.out.println("Spreading teams...");

        // Calculate team spawn locations
        Map<UHCTeam, Location> locations = new HashMap<>();
        for (UHCTeam team : TeamHandler.teams()) {
            Location randomSpawn = SpawnUtils.getRandomSpawn(getWorld(), properties.WORLDBORDER_START_SIZE.get());
            locations.put(team, randomSpawn);
        }

        // Verify that the teams are spread far enough apart
        for (UHCTeam team : locations.keySet()) {
            Location spawnLoc = locations.get(team);
            // Attempt 1,000 tries
            for (int i = 0; i < 1000; i++) {
                boolean clash = false;
                for (Location oL : locations.values()) {
                    if (oL.distanceSquared(spawnLoc) < Math.pow(minRadius, 2) && !oL.equals(spawnLoc)) {
                        System.out.println("CLASH! " + spawnLoc.toString() + " is too close to " + oL.toString());
                        clash = true;
                    }
                    if (!clash)
                        break;
                    spawnLoc = SpawnUtils.getRandomSpawn(getWorld(), properties.WORLDBORDER_START_SIZE.get());
                }
            }
            // Teleport everyone on the team to that location
            System.out.println(String.format("Teleporting players on team around %s to %.2f, %.2f, %.2f", team.getName(), spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));
            for (Player p : team.getPlayers().stream().map(Bukkit::getPlayer).filter(pl -> pl != null).collect(Collectors.toList())) {
                Location spawnAround = SpawnUtils.getSpawnAround(spawnLoc, 2);
                System.out.println(String.format("\tTeleporting %s to %.2f, %.2f, %.2f", p.getName(), spawnAround.getX(), spawnAround.getY(), spawnAround.getZ()));
                p.teleport(spawnAround);
            }
        }

        System.out.println("Spread teams!");
        System.out.println("Despawning players...");
        // Despawn all the players in attempt to prevent invisible players
        for (Player p : Bukkit.getOnlinePlayers()) {
            PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(p.getEntityId());
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(p.getUniqueId()))
                    continue;
                ((CraftPlayer) other).getHandle().playerConnection.sendPacket(destroyPacket);
            }
        }

        // Respawn everyone 5 ticks later
        Bukkit.getServer().getScheduler().runTaskLater(UHC.plugin, () -> {
            System.out.println("Respawning players");
            for (Player p : Bukkit.getOnlinePlayers()) {
                PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(((CraftPlayer) p).getHandle());
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (p.getUniqueId().equals(other.getUniqueId()))
                        continue;
                    ((CraftPlayer) other).getHandle().playerConnection.sendPacket(spawn);
                }
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void eatHead(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (event.getItem() != null)
                if (event.getItem().getType() == Material.SKULL_ITEM && event.getItem().getDurability() == 3) {
                    event.setCancelled(true);

                    if (event.getItem().getAmount() == 1)
                        event.getPlayer().getInventory().remove(event.getItem());
                    else {
                        event.getItem().setAmount(event.getItem().getAmount() - 1);
                        event.getPlayer().getInventory().setItemInMainHand(event.getItem());
                    }

                    Bukkit.getOnlinePlayers().forEach(p -> p.playSound(event.getPlayer().getLocation(), Sound.ENTITY_PLAYER_BURP, 1F, 1F));

                    event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 125, 1));
                    event.getPlayer().sendMessage(UtilChat.message("You have been given Regeneration"));
                }
        }
    }

    public void essentiallyDisable() {
        if (UHC.uhcNetwork != null)
            TeamHandler.teams().forEach(t -> new BotCommandRemoveTeam(UHC.plugin.serverId(), t.getName()).publish());

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setAllowFlight(false);
            p.setGameMode(GameMode.SURVIVAL);
            p.teleport(p.getLocation().add(0, 10, 0));
        }

        HandlerList.unregisterAll(UHC.plugin);

        Bukkit.getServer().getScheduler().cancelTasks(UHC.plugin);

        this.worldBorderHandler.setWorldborder(this.worldBorderHandler.getOverworld().getSize() + 150, 0);
        UHC.plugin.getPluginLoader().disablePlugin(UHC.plugin);
    }

    public void freeze() {
        int secondsPassed = Math.toIntExact((System.currentTimeMillis() - startTime) / 1000);

        System.out.println("Duration: " + properties.WORLDBORDER_TRAVEL_TIME.get() * 60);
        System.out.println("Seconds passed: " + secondsPassed);
        this.secondsRemaining = properties.WORLDBORDER_TRAVEL_TIME.get() * 60 - secondsPassed;

        System.out.println("Seconds remaining: " + secondsRemaining);
        overworldWorldborderSize = getWorld().getWorldBorder().getSize();
        getWorld().getWorldBorder().setSize(overworldWorldborderSize);
        System.out.println("Worldborder size: " + overworldWorldborderSize);

        freezeStartTime = System.currentTimeMillis();

        if (getNether() != null) {
            netherWorldborderSize = getNether().getWorldBorder().getSize();
            getNether().getWorldBorder().setSize(netherWorldborderSize);
        }
        this.state = FROZEN;
        Bukkit.getOnlinePlayers().stream().filter(p -> !TeamHandler.isSpectator(p)).forEach(FreezeHandler::freezePlayer);
        getWorld().getEntities().stream().filter(e -> e.getType() != EntityType.PLAYER).forEach(e -> FreezeHandler.frozenEntities.put(e, e.getLocation()));

        getWorld().setGameRuleValue("doDaylightCycle", "false");
        FreezeHandler.pvpEnabled = false;

        Bukkit.broadcastMessage(UtilChat.message("The game is now frozen"));
    }

    public void generate() {
        MOTDHandler.setMotd(ChatColor.DARK_RED + "Pregenerating world, come back soon!");
        state = State.GENERATING_WORLD;

        Integer worldborderSize = properties.WORLDBORDER_START_SIZE.get();
        int minX = getCenter().getBlockX() - worldborderSize;
        int maxX = getCenter().getBlockX() + worldborderSize;
        int minZ = getCenter().getBlockZ() - worldborderSize;
        int maxZ = getCenter().getBlockZ() + worldborderSize;

        generationTaskId = UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin,
                new GenerationTask(UHC.plugin, this, getWorld(), minX / 512, minZ / 512, maxX / 512, maxZ / 512), 1L, 1L);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer(net.md_5.bungee.api.ChatColor.RED + "We are pregenerating the world, come back later");
        }
    }

    public void generationComplete() {
        state = State.WAITING;
        MOTDHandler.setMotd(ChatColor.GRAY + "Pending initialization");
        Bukkit.broadcastMessage(UtilChat.message("World generation complete"));
        UHC.plugin.getServer().getScheduler().cancelTask(generationTaskId);
        initialize();
    }

    public Location getCenter() {
        return getWorld().getHighestBlockAt((int) properties.WORLDBORDER_CENTER.get().getX(), (int) properties.WORLDBORDER_CENTER.get().getZ()).getLocation();
    }

    public EndgameHandler getEndgameHandler() {
        return endgameHandler;
    }

    public World getNether() {
        return Bukkit.getWorld(getWorld().getName() + "_nether");
    }

    public ArenaProperties getProperties() {
        return properties;
    }

    public void setProperties(ArenaProperties properties) {
        this.properties = properties;
    }

    public TeamInventoryHandler getTeamInventoryHandler() {
        return teamInventoryHandler;
    }

    public World getWorld() {
        return Bukkit.getWorld(properties.WORLD.get());
    }

    public void initialize() {
        this.endgameHandler.reset();
        scoreboardUpdater.getScoreboard().createTeams();
        this.worldBorderHandler.setWorldborder(60);
        this.worldBorderHandler.setWarningDistance(0);
        getWorld().setGameRuleValue("doDaylightCycle", "false");
        getWorld().setGameRuleValue("doMobSpawning", "false");
        getWorld().setGameRuleValue("doMobLoot", "false");
        getWorld().setTime(1200);
        Bukkit.broadcastMessage(UtilChat.message("Initialized"));
        MOTDHandler.setMotd(ChatColor.GREEN + "Ready");
        players = new ArrayList<>();
        launchedFw = 0;
        winningTeamColor = Color.WHITE;
        state = State.INITIALIZED;
        queuedTeamRemovals.clear();
        uuidToStringMap.clear();
        logoutTimes.clear();
        players.clear();
        previouslyOpped.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void itemPickup(PlayerPickupItemEvent event) {
        // Display player head info
        if (!properties.DROP_PLAYER_HEAD.get())
            return;
        if (event.getItem().getItemStack().getType() == Material.SKULL_ITEM && event.getItem().getItemStack().getDurability() == 3) {
            Player player = event.getPlayer();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 0.5F);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ChatColor.BOLD + "=============================================");
            player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "You've picked up a player head!");
            if (properties.ENABLE_HEAD_APPLE.get()) {
                player.sendMessage(ChatColor.WHITE + "You can use this head to craft a Head Apple for healing");
                player.sendMessage(ChatColor.WHITE + "A golden head will give you 2x the effects of a golden apple!");
                player.sendRawMessage(ChatColor.GREEN + "To Craft: " + ChatColor.WHITE + "Use the recipe for a Golden Apple, but replace the apple with the head");
                player.sendMessage("");
                player.sendMessage(ChatColor.WHITE + "Optionally, right click the player head to eat it");
                player.sendMessage("");
            } else {
                player.sendMessage("");
                player.sendMessage("");
                player.sendMessage(ChatColor.WHITE + "Right click the head to restore some health");
                player.sendMessage("");
                player.sendMessage("");
            }
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ChatColor.BOLD + "=============================================");
        }
    }

    public void newTeam(String name, net.md_5.bungee.api.ChatColor color) {
        TeamHandler.registerTeam(name, new UHCPlayerTeam(name, color));
    }

    public void playerDisconnect(Player player) {
        UHCTeam teamForPlayer = TeamHandler.getTeamForPlayer(player);
        if (teamForPlayer == null || teamForPlayer instanceof TeamSpectator) {
            players.remove(player);
            return;
        }
        uuidToStringMap.put(player.getUniqueId(), player.getName());
        int reconnectMinutes = properties.REJOIN_MINUTES.get();
        int reconnectMs = 1000 * 60 * reconnectMinutes;
        logoutTimes.put(player.getUniqueId(), System.currentTimeMillis() + reconnectMs);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + player.getName() + " has disconnected! " + UtilTime.format(1, reconnectMs, UtilTime.TimeUnit.FIT) + " to rejoin!");
        }
    }

    public void playerJoin(Player player) {
        if (logoutTimes.containsKey(player.getUniqueId())) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + player.getName() + " has reconnected!");
            }
            this.logoutTimes.remove(player.getUniqueId());
        }
        if (queuedTeamRemovals.contains(player.getUniqueId())) {
            queuedTeamRemovals.remove(player.getUniqueId());
            TeamHandler.leaveTeam(player);
            players.remove(player);
            spectate(player);
            player.sendMessage(UtilChat.message("You have disconnected more than five minutes ago and have been removed from the game"));
        }
    }

    public Player[] players() {
        return players.toArray(new Player[players.size()]);
    }

    public boolean pvpDisabled() {
        return this.graceUntil != -1 && System.currentTimeMillis() < graceUntil;
    }

    public void removePlayer(Player player) {
        Iterator<Player> players = this.players.iterator();
        while (players.hasNext()) {
            if (players.next().getUniqueId().equals(player.getUniqueId()))
                players.remove();
        }
    }

    @Override
    public void run() {
        switch (state) {
            case WAITING:
            case INITIALIZED:
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setAllowFlight(true);
                }
                if (System.currentTimeMillis() > nextTipIn) {
                    displayTip();
                }
                break;
            case COUNTDOWN:
                if (countdownTask == null) {
                    countdownTask = new CountdownBarTask(System.currentTimeMillis() + 10000, 10000);
                    countdownTask.setTaskId(UHC.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(UHC.plugin, countdownTask, 0L, 1L));
                }
                if (countdown > 0) {
                    for (Player p : players) {

                        if (countdown <= 3) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1, 0.5F);
                            UtilTitle.title(p, ChatColor.RED + String.valueOf(countdown), "", 0, 25, 0);
                        } else {
                            UtilTitle.title(p, ChatColor.GOLD + "Starting in", ChatColor.RED + "" + countdown, 0, 25, 0);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1, 1);
                        }
                        p.sendMessage(UtilChat.message("Starting in " + ChatColor.GOLD + countdown));
                    }
                    MOTDHandler.setMotd(ChatColor.YELLOW + "Starting in " + countdown);
                    countdown--;
                } else {
                    start();
                }
                break;
            case RUNNING:
                if (pvpDisabled())
                    announcePvPGrace();
                MOTDHandler.setMotd(ChatColor.RED + "Game in progress. " + ChatColor.AQUA + "" + (players.size() - getSpectatorCount()) + ChatColor.RED + " alive");
                for (Player p : players) {
                    p.setGlowing(false);
                }
                if (teamCountLeft() <= 1 && properties.CHECK_ENDING.get()) {
                    if (teamsLeft().size() > 0) {
                        UHCPlayerTeam team = teamsLeft().get(0);
                        this.winningTeamColor = team.toColor();
                        stop(team.getFriendlyName());
                    }
                    state = ENDGAME;
                }
                if (worldBorderHandler.getOverworld().getSize() <= properties.WORLDBORDER_END_SIZE.get()) {
                    if (!notifiedDisabledSpawn) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1F, 1F);
                            p.sendMessage(UtilChat.message("Natural mob spawning has been cut by 75%"));
                        }

                        notifiedDisabledSpawn = true;
                    }
                    getWorld().getWorldBorder().setWarningDistance(0);
                }
                if (getNether() != null)
                    if (worldBorderHandler.netherTravelComplete())
                        getWorld().getWorldBorder().setWarningDistance(0);
                break;
            case ENDGAME:
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setAllowFlight(true);
                    p.getInventory().clear();
                    TeamHandler.leaveTeam(p);
                }
                if (this.launchedFw++ < 8) {
                    int distFromWB = 16;
                    double worldborderRadius = getWorld().getWorldBorder().getSize() / 2d;
                    Location pZX = getCenter().clone().add(worldborderRadius - distFromWB + (5 * Math.random()), 20, worldborderRadius - distFromWB + (5 * Math.random()));
                    Location pXnZ = getCenter().clone().add(worldborderRadius - distFromWB + (5 * Math.random()), 20, -(worldborderRadius - distFromWB + (5 * Math.random())));
                    Location pZnX = getCenter().clone().add(-(worldborderRadius - distFromWB) + (5 * Math.random()), 20, worldborderRadius - distFromWB + (5 * Math.random()));
                    Location nXZ = getCenter().clone().add(-(worldborderRadius - distFromWB) + (5 * Math.random()), 20, -(worldborderRadius - distFromWB + (5 * Math.random())));

                    Firework fw_pZX = (Firework) getWorld().spawnEntity(pZX, EntityType.FIREWORK);
                    Firework fw_pXnZ = (Firework) getWorld().spawnEntity(pXnZ, EntityType.FIREWORK);
                    Firework fw_pZnX = (Firework) getWorld().spawnEntity(pZnX, EntityType.FIREWORK);
                    Firework fw_nXZ = (Firework) getWorld().spawnEntity(nXZ, EntityType.FIREWORK);

                    FireworkMeta meta = fw_pZX.getFireworkMeta();
                    FireworkEffect.Type type = FireworkEffect.Type.BALL_LARGE;
                    FireworkEffect eff = FireworkEffect.builder().flicker(true).withColor(this.winningTeamColor).with(type).trail(true).build();
                    meta.addEffect(eff);
                    meta.setPower(1);
                    fw_pZX.setFireworkMeta(meta);
                    fw_pXnZ.setFireworkMeta(meta);
                    fw_pZnX.setFireworkMeta(meta);
                    fw_nXZ.setFireworkMeta(meta);
                    detonateFirework(fw_pZX);
                    detonateFirework(fw_pXnZ);
                    detonateFirework(fw_pZnX);
                    detonateFirework(fw_nXZ);
                }
                for (Player p : Bukkit.getOnlinePlayers())
                    p.setGlowing(false);
                break;
        }
        updateDisconnect();
    }

    public void saveToFile() {
        FileConfiguration cfg = new YamlConfiguration();
        // Save general information
        cfg.set("preset", properties.name);
        try {
            cfg.save(new File(UHC.plugin.getDataFolder(), "arena.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendEveryoneToTeamChannels() {
        if (UHC.uhcNetwork == null)
            return;
        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Bukkit.broadcastMessage(UtilChat.message("Creating discord channels..."));
//            new BotCommandCreateSpectator(UHC.plugin.serverId()).publishBlocking();
            TeamHandler.teams().forEach(t -> new BotCommandNewTeam(UHC.plugin.serverId(), t.getName()).publishBlocking());

            Bukkit.broadcastMessage(UtilChat.message("Moving everyone into their discord channels"));
            HashMap<UUID, String> teams = new HashMap<>();

            TeamHandler.teams().forEach(t -> t.getPlayers().forEach(u -> teams.put(u, t.getName())));
            new BotCommandAssignTeams(UHC.plugin.serverId(), null, teams).publishBlocking();

            Bukkit.broadcastMessage(UtilChat.message("Everyone should be moved"));
        });
    }

    public void setState(State state) {
        this.state = state;
    }

    public void spectate(Player player) {
        spectate(player, false);
    }

    public void spectate(Player player, boolean switchRole) {
        TeamHandler.joinTeam(TeamHandler.spectatorsTeam(), player);
        if (switchRole) {
            if (uhcNetwork != null) {
                new BotCommandAssignSpectator(UHC.plugin.serverId(), player.getUniqueId()).publish();
            }
        }
    }

    public void start() {
        GameListener.resetDeaths();
        this.endgameHandler.reset();

        this.worldBorderHandler.setWorldborder(properties.WORLDBORDER_START_SIZE.get());
        this.worldBorderHandler.setWarningDistance(WORLDBORDER_WARN_DIST);
        this.worldBorderHandler.setWorldborder(properties.WORLDBORDER_END_SIZE.get(), properties.WORLDBORDER_TRAVEL_TIME.get() * 60);

        getWorld().setGameRuleValue("naturalRegeneration", "false");
        getWorld().setGameRuleValue("doMobSpawning", "true");
        getWorld().setGameRuleValue("doMobLoot", "true");
        getWorld().setGameRuleValue("doDaylightCycle", "true");
        getWorld().setTime(0);

        PotionEffect resist = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30 * 20, 6, true, false);
        PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 30 * 20, 10, true, false);
        PotionEffect sat = new PotionEffect(PotionEffectType.SATURATION, 30 * 20, 20, true, false);

        for (Player p : players) {
            UtilTitle.title(p, null, ChatColor.GOLD + "The game has begun!", 10, 20 * 5, 20);
            p.sendMessage(UtilChat.message("The game has begun"));
            if (!TeamHandler.isSpectator(p)) {
                p.setGameMode(GameMode.SURVIVAL);
                p.setAllowFlight(false);
            }
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            if (!TeamHandler.isSpectator(p)) {
                p.getInventory().clear();
                for (PotionEffect e : p.getActivePotionEffects()) {
                    p.removePotionEffect(e.getType());
                }
            } else
                new SpecInventory(UHC.plugin, p);
            p.setBedSpawnLocation(getCenter(), true);
            p.addPotionEffects(Arrays.asList(resist, regen, sat));
            if (p.isOp()) {
                p.setOp(false);
                previouslyOpped.add(p);
            }
            p.getInventory().clear();
            p.closeInventory();
        }

        if (properties.SPREAD_PLAYERS.get())
            distributeTeams(properties.MIN_DISTANCE_BETWEEN_TEAMS.get());

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "xp -3000l @a");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "achievement take * @a");

        for (Entity e : getWorld().getEntities()) {
            if (e instanceof Tameable) {
                ((Tameable) e).setOwner(null);
                ((Tameable) e).setTamed(false);
            }
            if (e.getType() == EntityType.DROPPED_ITEM) {
                e.remove();
            }
        }

        for (Player p : players()) {
            if (properties.REGEN_TICKET_ENABLE.get())
                RegenTicket.give(p);
            if (properties.GIVE_COMPASS_ON_START.get())
                UHC.playerTracker.giveTracker(p);
        }

        sendEveryoneToTeamChannels();

        state = State.RUNNING;
        startTime = System.currentTimeMillis();

        if (UHC.plugin.getConfig().getBoolean("episodes.use"))
            UHC.markerHandler.startTracking();

        if (properties.PVP_GRACE_MINS.get() > 0) {
            this.graceUntil = System.currentTimeMillis() + (1000 * 60) * properties.PVP_GRACE_MINS.get();
            Bukkit.broadcastMessage(UtilChat.message(ChatColor.BOLD + "" + ChatColor.GOLD + "PVP is disabled for " +
                    UtilTime.format(1, graceUntil - System.currentTimeMillis(), UtilTime.TimeUnit.FIT)));
        }
    }

    public void startCountdown() {
        countdown = 10;
        state = COUNTDOWN;
    }

    public void stop(String winner) {
        this.winner = winner;
        this.endgameHandler.reset();
        this.teamInventoryHandler.reset();

        RegenTicket.clearRegenTickets();
        winner = WordUtils.capitalizeFully(winner.replace('_', ' '));

        this.worldBorderHandler.setWorldborder(60);
        this.worldBorderHandler.setWarningDistance(0);

        getWorld().setGameRuleValue("doDaylightCycle", "false");
        getWorld().setGameRuleValue("doMobSpawning", "false");
        getWorld().setGameRuleValue("doMobLoot", "false");

        getWorld().setTime(1200);
        getWorld().setThundering(false);
        getWorld().setStorm(false);

        for (Player p : players) {
            UtilTitle.title(p, ChatColor.GOLD + winner, ChatColor.GOLD + "has won the game", 10, 20 * 5, 20);
            p.teleport(getCenter());
            p.setGameMode(GameMode.SURVIVAL);
            p.setAllowFlight(true);
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.getInventory().clear();
            p.setGlowing(false);
            p.setFlySpeed(0.1f);
            p.setWalkSpeed(0.2f);
            for (PotionEffect f : p.getActivePotionEffects()) {
                p.removePotionEffect(f.getType());
            }
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 2);
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 0.5f);
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1);
            if (TeamHandler.isSpectator(p)) {
                for (Player olp : Bukkit.getOnlinePlayers()) {
                    olp.showPlayer(p);
                }
            }
        }

        for (Player p : previouslyOpped) {
            p.setOp(true);
        }

        bringEveryoneToLobby();

        MOTDHandler.setMotd(ChatColor.RESET + "" + ChatColor.RED + ChatColor.MAGIC + "|..|" + ChatColor.RESET + "  " + ChatColor.GOLD + winner + ChatColor.RED + " has won the game!  " + ChatColor.RED + ChatColor.MAGIC + "|..|");

        launchedFw = 0;

        UHC.markerHandler.stopTracking();
        state = ENDGAME;
    }

    public int teamCountLeft() {
        return teamsLeft().size();
    }

    public ArrayList<UHCPlayerTeam> teamsLeft() {
        ArrayList<UHCPlayerTeam> uniqueTeams = new ArrayList<>();
        for (Player p : players) {
            if (queuedTeamRemovals.contains(p.getUniqueId()))
                continue;
            UHCTeam team = TeamHandler.getTeamForPlayer(p);
            if (!(team instanceof UHCPlayerTeam))
                continue;
            UHCPlayerTeam pTeam = (UHCPlayerTeam) team;
            if (pTeam == TeamHandler.getTeamByName(TeamHandler.SPECTATORS_TEAM))
                continue;
            if (!uniqueTeams.contains(pTeam)) {
                uniqueTeams.add(pTeam);
            }
        }
        return uniqueTeams;
    }

    //todo: Remove me
    public void temp_FireworkLaunch(Color color) {
        this.launchedFw = 0;
        this.winningTeamColor = color;
        this.state = ENDGAME;
    }

    public void toggleShouldEndCheck() {
        this.properties.CHECK_ENDING.setValue(!this.properties.CHECK_ENDING.get());
        if (this.properties.CHECK_ENDING.get())
            Bukkit.broadcastMessage(UtilChat.message("Checking if the game should end"));
        else
            Bukkit.broadcastMessage(UtilChat.message("No longer checking if the game should end"));
    }

    public void toggleSpreadingPlayers() {
        this.properties.SPREAD_PLAYERS.setValue(!this.properties.SPREAD_PLAYERS.get());
        if (this.properties.SPREAD_PLAYERS.get()) {
            Bukkit.broadcastMessage(UtilChat.message("Spreading players once the game starts"));
        } else {
            Bukkit.broadcastMessage(UtilChat.message("No longer spreading players"));
        }
    }

    public void unfreeze() {
        if (secondsRemaining > 0) {
            getWorld().getWorldBorder().setSize(overworldWorldborderSize);
            getWorld().getWorldBorder().setSize(properties.WORLDBORDER_END_SIZE.get(), secondsRemaining);
            if (getNether() != null) {
                getNether().getWorldBorder().setSize(netherWorldborderSize);
                getNether().getWorldBorder().setSize(properties.WORLDBORDER_END_SIZE.get() * 2, secondsRemaining);
            }
        }
        this.state = RUNNING;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!TeamHandler.isSpectator(p)) {
                FreezeHandler.unfreeze(p);
            }
        }
        long frozenFor = System.currentTimeMillis() - freezeStartTime;
        System.out.println("Frozen for: " + frozenFor);
        this.startTime -= frozenFor;
        Bukkit.getServer().getScheduler().runTaskLater(UHC.plugin, () -> {
            FreezeHandler.restoreBlocks();
            FreezeHandler.pvpEnabled = true;
            Bukkit.broadcastMessage(UtilChat.message("Damage enabled"));
        }, 100);
        getWorld().setGameRuleValue("doDaylightCycle", "true");
        Bukkit.broadcastMessage(UtilChat.message("The game was frozen for " + ChatColor.GOLD + UtilTime.format(1, frozenFor, UtilTime.TimeUnit.FIT)));
        Bukkit.broadcastMessage(UtilChat.message("PvP will be enabled in 5 seconds"));
        FreezeHandler.frozenEntities.clear();
    }

    public void updateDisconnect() {
        Iterator<Map.Entry<UUID, Long>> iterator = logoutTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (System.currentTimeMillis() > entry.getValue()) {
                String playerName = uuidToStringMap.get(entry.getKey());
                Bukkit.broadcastMessage(generateBoldChat(playerName + " has been eliminated because they logged off 5 minutes ago!", net.md_5.bungee.api.ChatColor.WHITE).toLegacyText());
                queuedTeamRemovals.add(entry.getKey());
                iterator.remove();
                Iterator<Player> uhcPlayerIterator = players.iterator();
                while (uhcPlayerIterator.hasNext()) {
                    Player next = uhcPlayerIterator.next();
                    if (next.getUniqueId().equals(entry.getKey())) {
                        uhcPlayerIterator.remove();
                    }
                }
                uuidToStringMap.remove(entry.getKey());
            }
        }
    }

    private void craftingRecipes() {
        ShapedRecipe headApple = new ShapedRecipe(new ItemStack(Material.GOLDEN_APPLE, 1));
        headApple.shape("GGG", "GHG", "GGG");
        headApple.setIngredient('G', Material.GOLD_INGOT);
        headApple.setIngredient('H', new MaterialData(Material.SKULL_ITEM, (byte) 3));
        Bukkit.getServer().addRecipe(headApple);
    }

    private void detonateFirework(Firework firework) {
        ((CraftWorld) firework.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity) firework).getHandle(), (byte) 17);
        firework.remove();
    }

    private void displayTip() {
        String tip = tips[new Random().nextInt(tips.length)];
        if (tip.contains("%NAME%")) {
            tip = tip.replace("%NAME%", randomPlayer());
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Tip> " + ChatColor.RESET + tip);
            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1F, 1F);
        }
        nextTipIn = System.currentTimeMillis() + TIP_TIME;
    }

    private boolean entityExists(UUID u, World world) {
        for (Entity e : world.getEntities()) {
            if (e.getUniqueId().equals(u)) {
                return true;
            }
        }
        return false;
    }

    private int getSpectatorCount() {
        int count = 0;
        for (Player p : players) {
            if (TeamHandler.isSpectator(p))
                count++;
        }
        return count;
    }

    private int playerCount() {
        return this.players.size() - getSpectatorCount();
    }

    private String randomPlayer() {
        if (Bukkit.getOnlinePlayers().size() < 1) {
            return "PLAYERNOTFOUND";
        }
        return new ArrayList<>(Bukkit.getOnlinePlayers()).get(new Random().nextInt(Bukkit.getOnlinePlayers().size())).getName();
    }

    protected double[] worldborderLoc() {
        WorldBorder wb = getWorld().getWorldBorder();
        Location l = wb.getCenter();
        double locX = (wb.getSize() / 2) + l.getX();
        double locZ = (wb.getSize() / 2) + l.getZ();
        return new double[]{locX, locZ};
    }

    public enum State {
        INITIALIZED,
        GENERATING_WORLD,
        WAITING,
        COUNTDOWN,
        RUNNING,
        FROZEN,
        ENDGAME
    }

    public static class PlayerActionBarUpdater implements Runnable {

        @Override
        public void run() {
            if (UHC.arena.state == State.RUNNING) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (TeamHandler.isSpectator(p))
                        continue;
                    TextComponent bc;
                    if (p.getInventory().getItemInMainHand().getType() == Material.COMPASS && UHC.arena.getProperties().COMPASS_PLAYER_TRACKER.get()) {
                        double distance = UHC.playerTracker.distanceToTarget(p.getUniqueId());
                        if (Double.isInfinite(distance)) {
                            bc = (TextComponent) UtilChat.generateFormattedChat("Right click to find the closest target!", net.md_5.bungee.api.ChatColor.GOLD, 8);
                        } else if (distance == PlayerTrackerHandler.DIST_IN_OTHER_DIMENSION) {
                            bc = (TextComponent) UtilChat.generateFormattedChat("The player you are tracking is in another dimension!", net.md_5.bungee.api.ChatColor.RED, 8);
                        } else {
                            bc = (TextComponent) UtilChat.generateBoldChat(UHC.playerTracker.getTarget(p.getUniqueId()).getName(), net.md_5.bungee.api.ChatColor.GOLD);
                            bc.addExtra(UtilChat.generateBoldChat(" is ", net.md_5.bungee.api.ChatColor.DARK_GREEN));
                            bc.addExtra(UtilChat.generateBoldChat(Double.toString(distance), net.md_5.bungee.api.ChatColor.GOLD));
                            bc.addExtra(UtilChat.generateBoldChat(" blocks away!", net.md_5.bungee.api.ChatColor.DARK_GREEN));
                        }
                    } else {
                        Location l = p.getLocation();
                        bc = (TextComponent) UtilChat.generateFormattedChat("Current Position: ", net.md_5.bungee.api.ChatColor.GOLD, 0);
                        bc.addExtra(UtilChat.generateFormattedChat("X: ", net.md_5.bungee.api.ChatColor.RED, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getX()), net.md_5.bungee.api.ChatColor.GREEN, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(" Y: ", net.md_5.bungee.api.ChatColor.RED, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getY()), net.md_5.bungee.api.ChatColor.GREEN, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(" Z: ", net.md_5.bungee.api.ChatColor.RED, 0));
                        bc.addExtra(UtilChat.generateFormattedChat(String.format("%.2f", l.getZ()), net.md_5.bungee.api.ChatColor.GREEN, 0));
                    }
                    PacketPlayOutChat chat = new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + bc.toLegacyText() + "\"}"), (byte) 2);
                    ((CraftPlayer) p).getHandle().playerConnection.sendPacket(chat);
                }
            }
        }
    }
}
