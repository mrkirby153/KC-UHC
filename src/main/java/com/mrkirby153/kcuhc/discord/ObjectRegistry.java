package com.mrkirby153.kcuhc.discord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ObjectRegistry {

    /**
     * The main instance of the Object registry
     */
    public static ObjectRegistry INSTANCE = new ObjectRegistry();

    private HashSet<Deletable> deletables = new HashSet<>();

    /**
     * Prevent the object registry from being instantiated.
     */
    private ObjectRegistry() {

    }

    /**
     * Deletes all objects in the registry
     */
    public void delete() {
        // Create a copy of the list to prevent CMEs
        List<Deletable> immutableDeletes = new ArrayList<>(deletables);
        immutableDeletes.forEach(Deletable::delete);
        this.deletables.clear();
    }

    /**
     * Registers an object for automatic deletion
     *
     * @param deletable The object to register
     */
    public void register(Deletable deletable) {
        this.deletables.add(deletable);
    }

    /**
     * Unregisters an object for automatic deletion (Like if it was manually deleted)
     *
     * @param deletable The object to unregister
     */
    public void unregister(Deletable deletable) {
        this.deletables.remove(deletable);
    }
}
