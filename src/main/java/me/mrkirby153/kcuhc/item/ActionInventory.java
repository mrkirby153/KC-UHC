package me.mrkirby153.kcuhc.item;

import java.util.List;

public interface ActionInventory {

    String getName();

    List<ExecutableItem> getItems();

    int rows();
}
