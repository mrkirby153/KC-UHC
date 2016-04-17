package me.mrkirby153.kcuhc.item;

import java.util.HashMap;

public abstract class Gui implements ActionInventory {

    private final String name;
    private final int rows;

    private HashMap<Integer, ExecutableItem> items = new HashMap<>();

    public Gui(String name, int rows){
        this.name = name;
        this.rows = rows;
    }

    public ExecutableItem getStackInSlot(int slot){
        return this.items.get(slot);
    }

    public void setStackInSlot(ExecutableItem stack){
        items.put(stack.getSlot(), stack);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int rows() {
        return rows;
    }
}
