package me.mrkirby153.kcuhc.handler.listener;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.player.TeamInventoryModule;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTitle;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameListener implements Listener {

    private static final Random random = new Random();

    private TeamHandler teamHandler;
    private UHC plugin;

    public GameListener(TeamHandler teamHandler, UHC plugin) {
        this.teamHandler = teamHandler;
        this.plugin = plugin;
    }


    @EventHandler
    public void death(PlayerDeathEvent event) {
        if (!isRunning())
            return;
        Player dead = event.getEntity();
        UHCTeam team = teamHandler.getTeamForPlayer(dead);
        savePlayerData(dead);
        dead.setGlowing(false);
        teamHandler.leaveTeam(dead);
        for (Player p : UHC.getInstance().arena.players()) {
            UtilTitle.title(p, ChatColor.DARK_PURPLE + dead.getDisplayName(), ChatColor.AQUA + event.getDeathMessage().replace(dead.getName(), ""),
                    10, 20 * 5, 20);
        }
        dead.getWorld().strikeLightningEffect(dead.getLocation());
        killTamedHorses(dead);
        if (team != null && team.getPlayers().size() < 1) {
            System.out.println("--- TEAM ELIMINATED, DROPPING INVENTORY AT " + dead.getLocation() + " ---");
            ModuleRegistry.getLoadedModule(TeamInventoryModule.class).ifPresent(p -> p.dropInventory(team, dead.getLocation()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isRunning())
            return;
        plugin.arena.playerReconnect(event.getPlayer());
        event.setJoinMessage(ChatColor.BLUE + "Join> " + ChatColor.GRAY + event.getPlayer().getName());
        if (teamHandler.getTeamForPlayer(event.getPlayer()) == null)
            teamHandler.joinTeam(teamHandler.spectatorsTeam(), event.getPlayer());
        else
            teamHandler.joinTeam(teamHandler.getTeamForPlayer(event.getPlayer()), event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (!isRunning())
            return;
        event.setQuitMessage(ChatColor.BLUE + "Part> " + ChatColor.GRAY + event.getPlayer().getName());
        plugin.arena.playerDisconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!isRunning())
            return;
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }
        System.out.println(event.getEntity().getName() + " was damaged by " + event.getCause().toString());
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || event.getCause() == EntityDamageEvent.DamageCause.MAGIC || event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            return;
        }
        double oldDamage = event.getDamage();
        double newDamage = Math.floor(oldDamage / 2);
        if (newDamage < 1)
            newDamage = 1;
        event.setDamage(newDamage);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void entityDamage(EntityDamageByEntityEvent event) {
        if (!isRunning())
            return;
        if(event.getDamager().getType() == EntityType.PLAYER)
            return;
        double oldDamage = event.getDamage();
        double newDamage = Math.floor(oldDamage / 2);
        if (newDamage < 1)
            newDamage = 1;
        event.setDamage(newDamage);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!isRunning())
            return;
        SpectateListener.addEarlyPickup(event.getPlayer());
        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> plugin.arena.spectate(event.getPlayer(), true), 1L);
    }

    @EventHandler
    public void entityTame(EntityTameEvent event) {
        if (!isRunning())
            return;
        if (event.getEntityType() == EntityType.HORSE ) {
            Horse horse = (Horse) event.getEntity();
            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            UHCTeam team = teamHandler.getTeamForPlayer((Player) event.getOwner());
            if (team != null) {
                horse.setCustomName("Team " + team.getColor() + team.getTeamName() + "'s " + ChatColor.WHITE + "Horse");
            } else if (event.getOwner().getName().endsWith("s"))
                horse.setCustomName(event.getOwner().getName() + "' Horse");
            else
                horse.setCustomName(event.getOwner().getName() + "'s Horse");
        }
        if(event.getEntityType() == EntityType.DONKEY){
            Donkey donkey = (Donkey) event.getEntity();
            donkey.getInventory().setItem(0, new ItemStack(Material.SADDLE));
            UHCTeam team = teamHandler.getTeamForPlayer((Player) event.getOwner());
            if (team != null) {
                donkey.setCustomName("Team " + team.getColor() + team.getTeamName() + "'s " + ChatColor.WHITE + "Donkey");
            } else if (event.getOwner().getName().endsWith("s"))
                donkey.setCustomName(event.getOwner().getName() + "' Donkey");
            else
                donkey.setCustomName(event.getOwner().getName() + "'s Donkey");
        }
    }

    private void killTamedHorses(Player player) {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if(e.getType() == EntityType.DONKEY){
                    Donkey d = (Donkey) e;
                    if(d.getOwner() == null)
                        continue;
                    if(d.getOwner().getUniqueId().equals(player.getUniqueId())){
                        Entity passenger = d.getPassenger();
                        if(passenger != null && passenger.getType() == EntityType.PLAYER && !passenger.getUniqueId().equals(player.getUniqueId())){
                            passenger.sendMessage(UtilChat.message(ChatColor.GOLD + d.getOwner().getName() + "'s " + ChatColor.GRAY + "donkey has been re-tamed to you as they have died"));
                            d.setOwner((Player) passenger);
                            Bukkit.getServer().getPluginManager().callEvent(new EntityTameEvent(d, (Player) passenger));
                        } else {
                            d.setTamed(false);
                            d.setCustomName("");
                        }
                    }
                }
                if (e.getType() != EntityType.HORSE)
                    continue;
                Horse horse = (Horse) e;
                if (horse.getOwner() == null) {
                    continue;
                }
                if (horse.getOwner().getUniqueId().equals(player.getUniqueId())) {
                    // Check if a player is riding it
                    Entity passenger = horse.getPassenger();
                    if (passenger != null && passenger.getType() == EntityType.PLAYER && !passenger.getUniqueId().equals(player.getUniqueId())) {
                        // Re-tame the horse to them
                        passenger.sendMessage(UtilChat.message(ChatColor.GOLD + horse.getOwner().getName() + "'s " + ChatColor.GRAY + "horse has been re-tamed to you as they have died"));
                        horse.setOwner((Player) passenger);
                        Bukkit.getServer().getPluginManager().callEvent(new EntityTameEvent(horse, (Player) passenger));
                    } else {
                        horse.setTamed(false);
                        horse.setCustomName("");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityInteract(PlayerInteractEvent evt) {
        if (!isRunning())
            return;
        if (teamHandler.isSpectator(evt.getPlayer()))
            return;
        if (evt.getAction() == Action.RIGHT_CLICK_AIR || evt.getAction() == Action.RIGHT_CLICK_BLOCK)
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!teamHandler.isSpectator(p))
                    continue;
                Block clickedAgainst = evt.getClickedBlock();
                if (clickedAgainst == null)
                    return;
                BlockFace clickedFace = evt.getBlockFace();
                Block newBlock = clickedAgainst.getWorld().getBlockAt(clickedAgainst.getX() + clickedFace.getModX(), clickedAgainst.getY() + clickedFace.getModY(), clickedAgainst.getZ() + clickedFace.getModZ());
                // Check if there is a player at this block
                double dist = newBlock.getLocation().distanceSquared(p.getLocation());
                Location newLoc = p.getLocation().clone().add(0, 1, 0);
                double newDist = newBlock.getLocation().distanceSquared(newLoc);
                if (dist < 2 || newDist < 0.7225) {
                    Location loc = p.getLocation().clone();
                    double toAdd = 1.3;
                    if (newDist < 0.7225)
                        toAdd += 0.5;
                    loc.setY(loc.getY() + toAdd);
                    p.sendMessage(UtilChat.message("You are in the way of a player and have been moved"));
                    p.teleport(loc);
                }
            }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockPlace(BlockPlaceEvent evt) {
    }

    @EventHandler
    public void changeWorld(PlayerChangedWorldEvent event) {
        if (!isRunning())
            return;
        if (!event.getFrom().getName().contains("nether")) {
            return;
        }
        double bounds = plugin.arena.getWorld().getWorldBorder().getSize() / 2;
        System.out.println("Bounds: +/- " + bounds);
        Player player = event.getPlayer();
        if (Math.abs(player.getLocation().getBlockZ()) > bounds || Math.abs(player.getLocation().getBlockX()) > bounds) {
            System.out.println("Player has spawned outside the worldborder! Fixing");
            // Move the player diagonally into the worldborder
            Location toTeleport = player.getLocation().clone();
            System.out.println(String.format("Old Location: %s - %.2f, %.2f, %.2f", toTeleport.getWorld().getName(), toTeleport.getX(), toTeleport.getY(), toTeleport.getZ()));
            int attempts = 0;
            while (Math.abs(toTeleport.getBlockX()) > bounds - 2) {
                if (toTeleport.getX() < 0) {
                    toTeleport.setX(toTeleport.getX() + 0.5);
                } else {
                    toTeleport.setX(toTeleport.getX() - 0.5);
                }
                attempts++;
                if (attempts > 15000) {
                    attempts = 0;
                    System.out.println("GIVING UP ON X");
                    break; // Give up
                }
            }
            while (Math.abs(toTeleport.getBlockZ()) > bounds - 2) {
                if (toTeleport.getZ() < 0) {
                    toTeleport.setZ(toTeleport.getZ() + 0.5);
                } else {
                    toTeleport.setZ(toTeleport.getZ() - 0.5);
                }
                attempts++;
                if (attempts > 15000) {
                    System.out.println("GIVING UP ON Z");
                    break; // Give up
                }
            }
            toTeleport = event.getPlayer().getWorld().getHighestBlockAt(toTeleport).getLocation().add(0.5, 0.5, 0.5);
            System.out.println(String.format("New Location: %s - %.2f, %.2f, %.2f", toTeleport.getWorld().getName(), toTeleport.getX(), toTeleport.getY(), toTeleport.getZ()));
            event.getPlayer().teleport(toTeleport);
            event.getPlayer().sendMessage(UtilChat.message("You have been moved inside the world border"));
        }
    }


    @EventHandler
    public void spawnEvent(CreatureSpawnEvent event) {
        if (!isRunning())
            return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL)
            return;
        if (plugin.arena.getProperties().WORLDBORDER_END_SIZE.get() <= plugin.arena.getWorld().getWorldBorder().getSize()) {
            int num = random.nextInt(100);
            if (num < 75) {
                event.setCancelled(true);
            }
        }
    }

    private void savePlayerData(Player player) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("username", player.getName());
        cfg.set("uuid", player.getUniqueId().toString());
        cfg.set("deathLocation", player.getLocation());
        if (teamHandler.getTeamForPlayer(player) != null)
            cfg.set("team", teamHandler.getTeamForPlayer(player).getTeamName());
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            cfg.set("inv." + i, inv.getItem(i));
        }

        List<String> activeEffects = new ArrayList<>();
        for (PotionEffect e : player.getActivePotionEffects()) {
            activeEffects.add(e.getType().getName() + ":" + e.getDuration() + ":" + e.getAmplifier() + ":" + e.isAmbient() + ":" +
                    ((e.getColor() != null) ? e.getColor().asRGB() : "null") + ":" + e.hasParticles());
        }
        cfg.set("potions", activeEffects);
        try {
            cfg.save(new File(plugin.getDataFolder(), String.format("deaths/%s.yml", player.getUniqueId())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void restorePlayerData(Player player, boolean restoreLocation) {
        File file = new File(UHC.getInstance().getDataFolder(), String.format("deaths/%s.yml", player.getUniqueId()));
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        Location l = (Location) cfg.get("deathLocation");
        UHC.getInstance().teamHandler.leaveTeam(player);
        if (cfg.getString("team") != null)
            if (UHC.getInstance().teamHandler.getTeamByName(cfg.getString("team")) != null) {
                UHC.getInstance().teamHandler.joinTeam(UHC.getInstance().teamHandler.getTeamByName(cfg.getString("team")), player);
            }
        for (String key : cfg.getConfigurationSection("inv").getKeys(false)) {
            player.getInventory().setItem(Integer.parseInt(key), (ItemStack) cfg.get("inv." + key));
        }
        player.updateInventory();
        for (String pot : cfg.getStringList("potions")) {
            String[] parts = pot.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            int duration = Integer.parseInt(parts[1]);
            int amplifier = Integer.parseInt(parts[2]);
            boolean ambient = Boolean.parseBoolean(parts[3]);
            Color color = parts[4].equalsIgnoreCase("null") ? null : Color.fromBGR(Integer.parseInt(parts[4]));
            boolean hideParticles = Boolean.parseBoolean(parts[4]);
            PotionEffect eff = new PotionEffect(type, duration, amplifier, ambient, hideParticles, color);
            player.addPotionEffect(eff);
        }
        if (restoreLocation)
            player.teleport(l);
        file.delete();
    }

    public static boolean isDead(Player player) {
        return new File(UHC.getInstance().getDataFolder(), String.format("deaths/%s.yml", player.getUniqueId())).exists();
    }

    public static void resetDeaths() {
        try {
            FileUtils.deleteDirectory(new File(UHC.getInstance().getDataFolder(), "deaths"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: 6/5/2016 Check for lava/water/suffocation
    public static boolean validLocation(Player player) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(UHC.getInstance().getDataFolder(), String.format("deaths/%s.yml", player.getUniqueId())));
        Location location = (Location) cfg.get("deathLocation");
        WorldBorder wb = location.getWorld().getWorldBorder();
        double bounds = wb.getSize() / 2;
        if (location.getBlock().getType() != Material.AIR)
            return false;
        if (location.getBlock().getType() == Material.WATER || location.getBlock().getType() == Material.LAVA || location.getBlock().getType() == Material.STATIONARY_LAVA)
            return false;
        return Math.abs(location.getBlockX()) < bounds && Math.abs(location.getBlockZ()) < bounds;
    }

    private static boolean isRunning() {
        return UHC.getInstance().arena.currentState() == UHCArena.State.RUNNING || UHC.getInstance().arena.currentState() == UHCArena.State.FROZEN;
    }
}
