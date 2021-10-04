package com.mrkirby153.kcuhc.module.settings;

import me.mrkirby153.kcutils.Time;
import me.mrkirby153.kcutils.Time.TimeUnit;

import java.util.ArrayList;
import java.util.List;

public class TimeSetting extends ModuleSetting<Long> {

    private static final List<String> units = new ArrayList<>();

    static {
        for (Time.TimeUnit unit : Time.TimeUnit.values()) {
            if (unit != TimeUnit.FIT) {
                units.add(unit.getShortName());
            }
        }
    }


    public TimeSetting(String defaultValue) {
        super(Time.parse(defaultValue));
        this.internal = defaultValue;
    }

    @Override
    public Long parse(String s) throws SettingParseException {
        try {
            return Time.parse(s);
        } catch (IllegalArgumentException e) {
            throw new SettingParseException(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return this.getInternal();
    }

    @Override
    public List<String> getCompletions(String input) {
        // If the last character is a number suggest prefixes
        if (input.length() > 0) {
            char last = input.charAt(input.length() - 1);
            if (last >= '0' && last <= '9') {
                List<String> completions = new ArrayList<>();
                units.forEach(unit -> completions.add(String.format("%s%s", input, unit)));
                return completions;
            } else if (last == 'm') {
                return List.of(String.format("%ss", input));
            } else {
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }
}
