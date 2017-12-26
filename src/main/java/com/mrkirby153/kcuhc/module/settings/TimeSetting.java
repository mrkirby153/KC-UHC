package com.mrkirby153.kcuhc.module.settings;

import me.mrkirby153.kcutils.Time;

public class TimeSetting extends ModuleSetting<Long> {

    public TimeSetting(String defaultValue) {
        super(Time.INSTANCE.parse(defaultValue));
        this.internal = defaultValue;
    }

    @Override
    public Long parse(String s) throws SettingParseException {
        try {
            return Time.INSTANCE.parse(s);
        } catch (IllegalArgumentException e) {
            throw new SettingParseException(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return this.getInternal();
    }
}
