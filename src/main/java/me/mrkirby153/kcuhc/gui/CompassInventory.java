package me.mrkirby153.kcuhc.gui;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.TeamHandler;
import me.mrkirby153.kcuhc.arena.UHCTeam;
import me.mrkirby153.kcuhc.shop.Shop;
import me.mrkirby153.kcuhc.shop.item.Action;
import me.mrkirby153.kcuhc.shop.item.ShopItem;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompassInventory extends Shop<UHC> implements Runnable {


    private int page = 1;

    private static final int ROWS_PER_PAGE = 5;


    public CompassInventory(Player player) {
        this(player, 1);
    }

    public CompassInventory(Player player, int page) {
        super(UHC.plugin, player, 6, "Spectate");
        this.page = page;
        open();
    }

    @Override
    public void onOpen() {
        module.getServer().getScheduler().scheduleSyncRepeatingTask(module, this, 0L, 10L);
    }

    @Override
    public void build() {
        for (int i = 0; i < getInventory().getSize(); i++) {
            getInventory().setItem(i, null);
        }
        ArrayList<UHCTeam> teams = new ArrayList<>(TeamHandler.teams());
        int pages = (int) (teams.size() / ROWS_PER_PAGE == 0 ? 1.0D : Math.ceil((double) teams.size() / (double) ROWS_PER_PAGE));
        int currentRow = 0;
        int startIndex = ROWS_PER_PAGE * page - ROWS_PER_PAGE;
        int endIndex = ROWS_PER_PAGE * page;
        if (endIndex > teams.size())
            endIndex = teams.size();
        List<UHCTeam> uhcTeams = teams.subList(startIndex, endIndex);
        uhcTeams.sort((o1, o2) -> {
            if (o1.getPlayers().size() > o2.getPlayers().size()) {
                return -1;
            } else if (o1.getPlayers().size() < o2.getPlayers().size()) {
                return 1;
            } else {
                return 0;
            }
        });
        for (UHCTeam team : uhcTeams) {
            if (team == TeamHandler.spectatorsTeam())
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
                    if (team != TeamHandler.getTeamForPlayer((Player) op))
                        continue;
                    addButton(newSlot, playerItem((Player) op), new TeleportToPlayer((Player) op));
                    slot++;
                } else {
                    setItem(newSlot, new ShopItem(Material.SKULL_ITEM, ChatColor.GRAY + "Offline: " + op.getName()));
                    slot++;
                }
            }
            currentRow++;
        }
        if (page < pages)
            addButton(53, new ShopItem(Material.SIGN, (byte) 0, page + 1, "Page " + (page + 1), new String[0]), new PageChange(page + 1));
        if (page > 1)
            addButton(45, new ShopItem(Material.SIGN, (byte) 0, page - 1, "Page " + (page - 1), new String[0]), new PageChange(page - 1));
    }


    private void setItem(int slot, ItemStack item) {
        getInventory().setItem(slot, item);
    }

    private void setTeamWool(int row, UHCTeam team) {
        getInventory().setItem(row * 9, new ShopItem(Material.WOOL, getDye(team).getData(), 1, team.getColor() + WordUtils.capitalizeFully(team.getName()), new String[0]));
    }

    private ShopItem playerItem(Player player) {
        ShopItem item = new ShopItem(Material.SKULL_ITEM, (byte) 3, 1, player.getName(), new String[]{
                ChatColor.UNDERLINE + "Health:" + ChatColor.RESET + ChatColor.GOLD + " " + player.getHealth() + "/" + player.getMaxHealth(),
                ChatColor.UNDERLINE + "Food:" + ChatColor.RESET + ChatColor.GOLD + " " + player.getFoodLevel()
        });
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(player.getName());
        item.setItemMeta(meta);
        return item;
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

    @Override
    public void run() {
        build();
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
