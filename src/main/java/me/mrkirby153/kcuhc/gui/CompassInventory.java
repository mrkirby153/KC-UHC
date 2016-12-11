package me.mrkirby153.kcuhc.gui;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.team.TeamHandler;
import me.mrkirby153.kcuhc.team.UHCTeam;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.gui.Action;
import me.mrkirby153.kcutils.gui.Gui;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompassInventory extends Gui<UHC> implements Runnable {


    private static final int ROWS_PER_PAGE = 5;
    private int page = 1;
    private TeamHandler teamHandler;

    public CompassInventory(Player player, TeamHandler teamHandler) {
        this(player, 1, teamHandler);
    }

    public CompassInventory(Player player, int page, TeamHandler teamHandler) {
        super(UHC.plugin, player, 6, "Spectate");
        this.page = page;
        this.teamHandler = teamHandler;
        open();
    }

    @Override
    public void build() {
        for (int i = 0; i < getInventory().getSize(); i++) {
            getInventory().setItem(i, null);
        }
        ArrayList<UHCTeam> teams = new ArrayList<>(teamHandler.teams());
        int pages = (int) (teams.size() / ROWS_PER_PAGE == 0 ? 1.0D : Math.ceil((double) teams.size() / (double) ROWS_PER_PAGE));
        int currentRow = 0;
        int startIndex = ROWS_PER_PAGE * page - ROWS_PER_PAGE;
        int endIndex = ROWS_PER_PAGE * page;
        if (endIndex > teams.size())
            endIndex = teams.size();
        List<UHCTeam> uhcTeams = teams.subList(startIndex, endIndex);
        uhcTeams.sort((o1, o2) -> {
            return o2.getPlayers().size() - o1.getPlayers().size();
        });
        for (UHCTeam team : uhcTeams) {
            if (team == teamHandler.spectatorsTeam())
                continue;
            setTeamWool(currentRow, team);
            int slot = 1;
            for (UUID uuid : team.getPlayers()) {
                boolean playerInArena = false;
                for (Player p : UHC.arena.players()) {
                    if (p.getUniqueId().equals(uuid))
                        playerInArena = true;
                }
                if (!playerInArena)
                    continue;
                // Find some way for multi line paginated
                if (slot > 8) {
                    continue;
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op == null)
                    continue;
                int newSlot = currentRow * 9 + slot;
                if (op instanceof Player) {
                    if (team != teamHandler.getTeamForPlayer((Player) op))
                        continue;
                    addButton(newSlot, playerItem((Player) op), new TeleportToPlayer((Player) op));
                    slot++;
                } else {
                    setItem(newSlot, new ItemFactory(Material.SKULL_ITEM).name(ChatColor.GRAY + "OFFLINE: " + op.getName()).construct());
                    slot++;
                }
            }
            currentRow++;
        }
        if (page < pages) {
            addButton(53, new ItemFactory(Material.SIGN).name("Page " + (page + 1)).amount(page + 1).construct(), new PageChange(page + 1));
        }
        if (page > 1)
            addButton(45, new ItemFactory(Material.SIGN).name("Page " + (page - 1)).amount(page - 1).construct(), new PageChange(page - 1))
                    ;
    }

    @Override
    public void onOpen() {
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 10L);
    }

    @Override
    public void run() {
        build();
    }

    private DyeColor getDye(UHCTeam team) {
        switch (team.getColor()) {
            case BLACK:
                return DyeColor.BLACK;
            case DARK_BLUE:
                return DyeColor.BLUE;
            case BLUE:
                return DyeColor.CYAN;
            case DARK_GREEN:
                return DyeColor.GREEN;
            case GREEN:
                return DyeColor.LIME;
            case DARK_AQUA:
                return DyeColor.BLUE;
            case DARK_RED:
                return DyeColor.RED;
            case DARK_PURPLE:
                return DyeColor.MAGENTA;
            case GOLD:
                return DyeColor.YELLOW;
            case GRAY:
                return DyeColor.SILVER;
            case DARK_GRAY:
                return DyeColor.GRAY;
            case AQUA:
                return DyeColor.CYAN;
            case RED:
                return DyeColor.RED;
            case LIGHT_PURPLE:
                return DyeColor.MAGENTA;
            case YELLOW:
                return DyeColor.YELLOW;
            case WHITE:
                return DyeColor.WHITE;
            default:
                return DyeColor.WHITE;
        }
    }

    private ItemStack playerItem(Player player) {
        ItemStack construct = new ItemFactory(Material.SKULL_ITEM).data(3).name(player.getName()).lore(ChatColor.GREEN + "Health: " + ChatColor.RESET + player.getHealth() + "/" + player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(),
                ChatColor.GREEN + "Food: " + ChatColor.RESET + player.getFoodLevel()).construct();
        SkullMeta meta = (SkullMeta) construct.getItemMeta();
        meta.setOwner(player.getName());
        construct.setItemMeta(meta);
        return construct;
    }

    private void setItem(int slot, ItemStack item) {
        getInventory().setItem(slot, item);
    }

    private void setTeamWool(int row, UHCTeam team) {
        getInventory().setItem(row * 9, new ItemFactory(Material.WOOL).data(getDye(team).getWoolData()).name(team.getColor()+WordUtils.capitalizeFully(team.getFriendlyName())).construct());
    }

    private class TeleportToPlayer implements Action {

        private Player player;

        public TeleportToPlayer(Player toTeleportTo) {
            this.player = toTeleportTo;
        }

        @Override
        public void onClick(Player player, ClickType clickType) {
            player.teleport(this.player);
            player.closeInventory();
        }
    }

    private class PageChange implements Action {

        private int nextPage;

        public PageChange(int nextPage) {
            this.nextPage = nextPage;
        }

        @Override
        public void onClick(Player player, ClickType clickType) {
            page = nextPage;
            build();
        }
    }
}
