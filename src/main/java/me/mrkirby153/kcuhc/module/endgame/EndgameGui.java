package me.mrkirby153.kcuhc.module.endgame;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.gui.admin.GameSettingsInventory;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.ItemFactory;
import me.mrkirby153.kcutils.gui.Action;
import me.mrkirby153.kcutils.gui.Gui;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class EndgameGui extends Gui<UHC> {

    public EndgameGui(UHC uhc, Player player) {
        super(uhc, player, (int) Math.ceil(EndgameScenarioModule.scenarios().size() / 9D)+1, "Endgame Scenario");
        open();
    }

    @Override
    public void build() {
        getInventory().clear();
        actions.clear();
        int slot = 0;
        for(EndgameScenario m : EndgameScenarioModule.scenarios()){
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
            addButton(slot++, factory.construct(), new LoadScenario(m, this));
        }
        double a = slot / 9D;
        addButton((int) ((Math.ceil(a) * 9) + 8), new ItemFactory(Material.ARROW).name("Back").construct(), ((player, clickType) -> new GameSettingsInventory(plugin, player)));
    }

    private static class LoadScenario implements Action {

        private EndgameScenario scenario;
        private EndgameGui parent;

        public LoadScenario(EndgameScenario scenario, EndgameGui parent) {
            this.scenario = scenario;
            this.parent = parent;
        }

        @Override
        public void onClick(Player player, ClickType clickType) {
            EndgameScenarioModule.loadScenario(scenario.getClass());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 2F);
            parent.build();
        }
    }
}
