package com.mrkirby153.kcuhc.discord;

import java.util.ArrayList;

public class ObjectRegistry {

    public static ObjectRegistry INSTANCE = new ObjectRegistry();

    private ArrayList<IDeletable> deletables = new ArrayList<>();

    private ObjectRegistry() {

    }

    /**
     * Deletes all objects tracked by the registry
     */
    public void delete() {
        this.deletables.forEach(IDeletable::delete);
    }

    /**
     * Registers an object to be automatically deleted
     *
     * @param deletable The object to delete
     */
    public void registerForDelete(IDeletable deletable) {
        this.deletables.add(deletable);
    }
}
