package me.mrkirby153.kcuhc.module;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.gui.admin.GameAdminInventory;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.gui.Action;
import me.mrkirby153.kcutils.gui.Gui;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class ModuleGui extends Gui<UHC> {

    public ModuleGui(UHC uhc, Player player) {
        super(uhc, player, (int) Math.ceil((ModuleRegistry.allModules().size() + 1) / 9D) + 1, "UHC Modules");
        open();
    }

    private static int makeNine(int number) {
        while (number % 9 != 0) {
            number++;
        }
        return number;
    }

    @Override
    public void build() {
        getInventory().clear();
        actions.clear();
        int slot = 0;
        boolean enabledModules = true;
        for (UHCModule m : ModuleRegistry.allModules()) {
            if (!m.isLoaded() && enabledModules) {
                enabledModules = false;
                addButton(slot++, new ItemFactory(Material.STAINED_GLASS_PANE).name("<- Loaded ; Unloaded -> ").data(7).construct(), null);
            }
            ItemFactory factory = m.getGuiIcon();
            if (m.isLoaded())
                factory.glowing();
            if(m.getDepends().size() > 0) {
                factory.lore("", ChatColor.AQUA + "Loads: ");
                for (UHCModule mod : m.getDepends()) {
                    factory.lore(" - " + mod.getName());
                }
                factory.lore("");
            }
            factory.lore((m.isLoaded() ? ChatColor.GREEN + "Loaded" : ChatColor.RED + "Unloaded"));
            if (m.getDescription() != null && !m.getDescription().isEmpty())
                factory.lore("", m.getDescription());
            addButton(slot++, factory.construct(), new ToggleModule(m, this));
        }
        double a = slot / 9D;
        addButton((int) ((Math.ceil(a) * 9) + 8), new ItemFactory(Material.ARROW).construct(), ((player, clickType) -> new GameAdminInventory(plugin, player)));
    }

    public static class ToggleModule implements Action {

        private final UHCModule module;
        private final ModuleGui parent;

        public ToggleModule(UHCModule module, ModuleGui parent) {
            this.module = module;
            this.parent = parent;
        }


        @Override
        public void onClick(Player player, ClickType clickType) {
            if (module.isLoaded())
                ModuleRegistry.unloadModule(module);
            else {
                ModuleRegistry.loadModule(module);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
            parent.build();
        }
    }
}
