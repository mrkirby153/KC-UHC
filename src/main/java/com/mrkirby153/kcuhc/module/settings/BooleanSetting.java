package com.mrkirby153.kcuhc.module.settings;

public class BooleanSetting extends ModuleSetting<Boolean> {

    public BooleanSetting(Boolean defaultValue) {
        super(defaultValue);
    }

    @Override
    public Boolean parse(String s) throws SettingParseException {
        return Boolean.parseBoolean(s);
    }
}
