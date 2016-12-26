package me.mrkirby153.kcuhc.module.endgame;

import me.mrkirby153.kcuhc.UHC;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

public class EndgameScenarioModule extends UHCModule {

    private static HashSet<EndgameScenario> availableScenarios = new HashSet<>();
    private static EndgameScenario currentScenario;

    public EndgameScenarioModule() {
        super(Material.CAKE, 0, "Endgame Scenario", true, "Select the scenario to end the game");
    }

    public static EndgameScenario currentScenario() {
        return currentScenario;
    }

    public static <T extends EndgameScenario> Optional<T> getScenario(Class<T> scenario) {
        for (EndgameScenario s : availableScenarios) {
            if (s.getClass().equals(scenario))
                return Optional.of(scenario.cast(s));
        }
        return Optional.empty();
    }

    public static void loadScenario(Class<? extends EndgameScenario> scenario) {
        getScenario(scenario).ifPresent(scen -> {
            if (currentScenario != null)
                currentScenario.unload();
            currentScenario = scen;
            currentScenario.load();
        });
    }

    public static void registerScenario(EndgameScenario scenario) {
        availableScenarios.add(scenario);
    }

    public static Collection<EndgameScenario> scenarios() {
        return availableScenarios;
    }

    public static void setDefault(Class<? extends EndgameScenario> defaultScenario) {
        getScenario(defaultScenario).ifPresent(scen -> {
            currentScenario = scen;
        });
    }

    @Override
    public void onDisable() {
        if (currentScenario != null)
            if (currentScenario.isLoaded())
                currentScenario.unload();
    }

    @Override
    public void onEnable() {
        if (currentScenario != null)
            if (!currentScenario.isLoaded())
                currentScenario.load();
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.TICK)
            return;
        if (currentScenario != null && UHC.getInstance().arena.currentState() == UHCArena.State.RUNNING)
            currentScenario.update();
    }
}
