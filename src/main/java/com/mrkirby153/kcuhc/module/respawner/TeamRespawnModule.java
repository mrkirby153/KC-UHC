package com.mrkirby153.kcuhc.module.respawner;

import com.google.inject.Inject;
import com.mrkirby153.kcuhc.UHC;
import com.mrkirby153.kcuhc.game.event.GameStartingEvent;
import com.mrkirby153.kcuhc.game.event.GameStoppingEvent;
import com.mrkirby153.kcuhc.module.UHCModule;
import com.mrkirby153.kcuhc.module.respawner.TeamRespawnStructure.Phase;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;

public class TeamRespawnModule extends UHCModule {

    public TeamRespawnStructure structure;
    private RespawnerCommand respawnerCommand;
    private UHC plugin;
    private BukkitTask bt;

    private Location location;

    @Inject
    public TeamRespawnModule(UHC uhc) {
        super("Team Respawn", "Creates a structure to respawn teammates", Material.NETHER_STAR);
        UHC.getCommandManager().registerCommand(respawnerCommand = new RespawnerCommand(uhc, this));

        this.plugin = uhc;
    }

    @Override
    public void onLoad() {
        bt = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            if (structure != null) {
                structure.tick();
            }
        }, 0, 2);
        this.location = this.plugin.getConfig()
            .getObject("modules.respawn.location", Location.class);
        if (this.location != null) {
            this.plugin.getLogger().info("[RESPAWNER] Loading respawn at " + this.location);
            this.structure = new TeamRespawnStructure(this.plugin, this.location);
            this.structure.buildStructure();
        }
    }

    @Override
    public void onUnload() {
        if (bt != null) {
            bt.cancel();
        }
    }

    @EventHandler
    public void onTick(UpdateEvent e) {
        if (e.getType() == UpdateType.TICK) {
            if (structure != null) {
                structure.tick();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Prevent the beacon from being broken
        if (this.structure == null) {
            return; // Structure does not exist in the world
        }
        StructureLocation structure = this.getStructureBounds();

        Location brokenBlock = event.getBlock().getLocation();
        if (brokenBlock.toVector()
            .isInAABB(structure.min(), structure.max())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (this.structure == null) {
            return; // Structure does not exist in the world
        }

        StructureLocation structure = this.getStructureBounds();

        if (event.getClickedBlock() != null && event.getClickedBlock().getLocation().toVector()
            .isInAABB(structure.min(), structure.max())) {
            event.setCancelled(true);
            if (event.getClickedBlock().getType() == Material.CHEST
                && this.structure.getPhase() == Phase.IDLE && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.getPlayer().playSound(event.getClickedBlock().getLocation(),
                    Sound.BLOCK_CHEST_LOCKED, 1.0F, 1.0F);
                event.getPlayer().openInventory(this.structure.getInventory());
                this.structure.addObserver(event.getPlayer());
            }
        }
        // TODO 5/10/2019: Prevent soul vials from being thrown
        if (event.getAction() == Action.RIGHT_CLICK_AIR
            || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (SoulVialHandler.getInstance().isSoulVial(event.getItem())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getSource().equals(this.structure.getInventory())) {
            event.setCancelled(true);
        }
        if (event.getDestination().equals(this.structure.getInventory()) && !SoulVialHandler
            .getInstance().isSoulVial(event.getItem())) {
            event.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory ci = event.getClickedInventory();
        if (ci != null && ci.equals(this.structure.getInventory()) && !SoulVialHandler.getInstance()
            .isSoulVial(event.getCursor())) {
            event.setCancelled(true);
        }
        // Prevent Shift clicking
        if (event.isShiftClick() && event.getInventory().equals(this.structure.getInventory())
            && !SoulVialHandler.getInstance().isSoulVial(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(this.structure.getInventory())) {
            this.structure.removeObserver((Player) event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStarting(GameStartingEvent event) {
        this.structure.setPhase(Phase.IDLE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStopping(GameStoppingEvent event) {
        this.structure.setPhase(Phase.DEACTIVATED);
        SoulVialHandler.getInstance().clearSoulVials();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ItemStack soulVial = SoulVialHandler.getInstance().getSoulVial(player);
        player.getLocation().getWorld().dropItem(player.getLocation(), soulVial);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        StructureLocation loc = this.getStructureBounds();
        if (loc != null) {
            event.blockList().removeIf(b -> loc.in(b.getLocation().toVector()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        StructureLocation loc = this.getStructureBounds();
        if (loc != null) {
            event.blockList().removeIf(b -> loc.in(b.getLocation().toVector()));
        }
    }

    @Nullable
    private StructureLocation getStructureBounds() {
        if (this.structure == null) {
            return null;
        }
        Location center = this.structure.getCenter();
        int minX, maxX, minY, maxY, minZ, maxZ;
        minX = center.getBlockX() - TeamRespawnStructure.STRUCTURE_SIZE;
        maxX = center.getBlockX() + TeamRespawnStructure.STRUCTURE_SIZE;

        minY = center.getBlockY();
        maxY = center.getBlockY() + TeamRespawnStructure.STRUCTURE_HEIGHT;

        minZ = center.getBlockZ() - TeamRespawnStructure.STRUCTURE_SIZE;
        maxZ = center.getBlockZ() + TeamRespawnStructure.STRUCTURE_SIZE;
        return new StructureLocation(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private class StructureLocation {

        private final int minX, minY, minZ, maxX, maxY, maxZ;

        public StructureLocation(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public boolean in(Vector v) {
            return v.isInAABB(min(), max());
        }

        public Vector min() {
            return new Vector(minX, minY, minZ);
        }

        public Vector max() {
            return new Vector(maxX, maxY, maxZ);
        }

    }
}
