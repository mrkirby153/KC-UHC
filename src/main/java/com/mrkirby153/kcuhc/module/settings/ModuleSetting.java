package com.mrkirby153.kcuhc.module.settings;

import java.util.ArrayList;
import java.util.List;

public abstract class ModuleSetting<T> {

    private final T defaultValue;
    protected T value;

    protected String internal;

    public ModuleSetting(T defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.internal = defaultValue.toString();
    }

    public abstract T parse(String s) throws SettingParseException;

    /**
     * Sets the setting
     *
     * @param s The string to parse
     *
     * @throws SettingParseException If there is an error prasing
     */
    public final void set(String s) throws SettingParseException {
        this.value = parse(s);
        this.internal = s;
    }

    /**
     * Gets the current value of the setting
     *
     * @return The setting's value
     */
    public T getValue() {
        return this.value;
    }

    /**
     * Gets the setting to display to the user
     *
     * @return The string to display
     */
    @Override
    public String toString() {
        return getValue().toString();
    }

    /**
     * Gets a list of completions to show for this module
     *
     * @return The list of completions
     */
    public List<String> getCompletions(String input) {
        return new ArrayList<>();
    }

    /**
     * Gets the internal value used for saving to the preset
     *
     * @return The value to save to the preset confing
     */
    public final String getInternal() {
        return this.internal;
    }

    /**
     * Resets this setting to its default value
     */
    public final void reset() {
        this.value = this.defaultValue;
    }
}
