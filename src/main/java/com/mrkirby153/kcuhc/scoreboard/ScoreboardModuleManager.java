package com.mrkirby153.kcuhc.scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Class to manage scoreboard modules
 */
public class ScoreboardModuleManager {

    public static final ScoreboardModuleManager INSTANCE = new ScoreboardModuleManager();

    private final Map<ScoreboardModule, Integer> scoreboardModules = new ConcurrentHashMap<>();

    private final AtomicInteger priority = new AtomicInteger(-1);


    /**
     * Adds a module to the scoreboard. Modules installed using this method are guaranteed to be
     * in-order
     *
     * @param module The module to add
     */
    public void installModule(ScoreboardModule module) {
        installModule(module, priority.getAndDecrement());
    }

    /**
     * Adds a module to the scoreboard
     *
     * @param module   The module to add
     * @param priority The priority of the module
     */
    public void installModule(ScoreboardModule module, int priority) {
        scoreboardModules.put(module, priority);
    }

    /**
     * Removes a module from the scoreboard
     *
     * @param module The module to remove
     */
    public void removeModule(ScoreboardModule module) {
        scoreboardModules.remove(module);
    }

    /**
     * Removes all modules of a given class from the scoreboard
     *
     * @param clazz The module to remove
     */
    public void removeModule(Class<? extends ScoreboardModule> clazz) {
        scoreboardModules.entrySet()
            .removeIf(entry -> clazz.isAssignableFrom(entry.getKey().getClass()));
    }

    /**
     * Gets a list of installed modules sorted by their priority
     *
     * @return A list of installed modules
     */
    public List<ScoreboardModule> getModules() {
        List<Entry<ScoreboardModule, Integer>> list = new ArrayList<>(scoreboardModules.entrySet());
        list.sort(Entry.comparingByValue());
        Collections.reverse(list);
        return list.stream().map(Entry::getKey).collect(Collectors.toList());
    }
}
