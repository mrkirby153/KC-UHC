package me.mrkirby153.kcuhc.module.worldborder;

import me.mrkirby153.kcuhc.arena.GameStateChangeEvent;
import me.mrkirby153.kcuhc.arena.UHCArena;
import me.mrkirby153.kcuhc.module.ModuleRegistry;
import me.mrkirby153.kcuhc.module.UHCModule;
import me.mrkirby153.kcuhc.utils.UtilChat;
import me.mrkirby153.kcuhc.utils.UtilTime;
import me.mrkirby153.kcutils.event.UpdateEvent;
import me.mrkirby153.kcutils.event.UpdateType;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class EndgameModule extends UHCModule {

    protected long nextEndgamePhaseOn = -1;
    protected int currEgPhase = EndgamePhase.NORMALGAME.ordinal();

    private double lastSecond;

    private boolean running = false;

    private long nextWBShrink = -1;

    public EndgameModule() {
        super(Material.BARRIER, 0, "Endgame Worldborder", true, "Shrinks the world border to one block");
        addDepends(WorldBorderModule.class);
    }

    public void activateNextPhase() {
        if (currEgPhase + 1 == EndgamePhase.values().length)
            return;
        currEgPhase++;
        BaseComponent text = UtilChat.generateBoldChat(getCurrentEndgamePhase().name, net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);
        text.addExtra(UtilChat.generateBoldChat(" active!", net.md_5.bungee.api.ChatColor.DARK_RED));
        for (Player p : getPlugin().arena.players()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1F, 1F);
            p.spigot().sendMessage(text);
        }
    }

    public void announce(double secondsUntil, boolean force) {
        if (((secondsUntil < 10 || (secondsUntil % 60) == 0 || secondsUntil == 30) && secondsUntil != lastSecond) || force) {
            lastSecond = secondsUntil;
            BaseComponent text = UtilChat.generateBoldChat(getNextEndgamePhase().getName(), ChatColor.LIGHT_PURPLE);
            text.addExtra(UtilChat.generateBoldChat(" in " + UtilTime.format(1, (long) (secondsUntil * 1000), UtilTime.TimeUnit.FIT), ChatColor.GREEN));
            for (Player p : getPlugin().arena.players()) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1F);
                p.spigot().sendMessage(text);
            }
        }
    }

    public void endgameAction() {
        switch (getCurrentEndgamePhase()) {
            case SHRINKING_WORLDBORDER:
                if (System.currentTimeMillis() > nextWBShrink) {
                    ModuleRegistry.getLoadedModule(WorldBorderModule.class).ifPresent(wb -> {
                        if(wb.getOverworldBorder().getSize() > 1){
                            wb.setWorldBorder(wb.getOverworldBorder().getSize() - 2, 1);
                        }
                        nextWBShrink = System.currentTimeMillis() + 3000;
                    });
                }
                break;
        }
    }

    public EndgamePhase getCurrentEndgamePhase() {
        return EndgamePhase.values()[currEgPhase];
    }

    @Override
    public void onEnable() {
        Bukkit.broadcastMessage(UtilChat.message("Endgame enabled!"));
        reset();
    }

    @Override
    public void onDisable() {
        Bukkit.broadcastMessage(UtilChat.message("Endgame disabled!"));
    }

    public void setCurrentEndgamePhase(EndgamePhase currentEndgamePhase) {
        currEgPhase = currentEndgamePhase.ordinal();
    }

    public EndgamePhase getNextEndgamePhase() {
        if (currEgPhase + 1 >= EndgamePhase.values().length)
            return EndgamePhase.UNKNOWN;
        return EndgamePhase.values()[currEgPhase + 1];
    }

    public long getNextEndgamePhaseOn() {
        return nextEndgamePhaseOn;
    }

    public void reset() {
        this.nextEndgamePhaseOn = -1;
        this.currEgPhase = EndgamePhase.NORMALGAME.ordinal();
        this.running = false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getTo() == UHCArena.State.RUNNING)
            reset();
        if (event.getTo() == UHCArena.State.INITIALIZED)
            reset();
        if (event.getTo() == UHCArena.State.ENDGAME) {
            reset();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.TICK)
            return;
        if (shouldStart() && !running)
            start();
        if (nextEndgamePhaseOn == -1)
            return;
        long time = nextEndgamePhaseOn - System.currentTimeMillis();
        double secondsUntil = Math.ceil(time / 1000D);
        if (secondsUntil > 0)
            announce(secondsUntil, false);
        if (time <= 0)
            activateNextPhase();
        if (running)
            endgameAction();
    }

    public void setNextEndgamePhaseIn(long nextEndgamePhaseIn) {
        this.nextEndgamePhaseOn = nextEndgamePhaseIn;
    }

    public boolean shouldStart() {
        return getPlugin().arena.currentState() == UHCArena.State.RUNNING && getPlugin().arena.getWorld().getWorldBorder().getSize() <= getPlugin().arena.getProperties().WORLDBORDER_END_SIZE.get();
    }

    public void start() {
        System.out.println("=== [ STARTED ENDGAME AT " + UtilTime.now() + " ] ===");
        nextEndgamePhaseOn = System.currentTimeMillis() + getNextEndgamePhase().duration;
        running = true;
        long timeLeft = nextEndgamePhaseOn - System.currentTimeMillis();
        System.out.println("\t- Next Phase in " + UtilTime.format(1, timeLeft, UtilTime.TimeUnit.FIT));
        announce(Math.floor(timeLeft) / 1000, true);
    }

    public enum EndgamePhase {
        UNKNOWN("Unknown", -1),
        NORMALGAME("Normal", -1),
        SHRINKING_WORLDBORDER("Shrinking Worldborder", 600000);
        private long duration;
        private String name;

        EndgamePhase(String friendlyName, long duration) {
            this.duration = duration;
            this.name = friendlyName;
        }

        public long getDuration() {
            return duration;
        }

        public String getName() {
            return name;
        }
    }
}
