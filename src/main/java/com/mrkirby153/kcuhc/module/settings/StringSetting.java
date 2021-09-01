package com.mrkirby153.kcuhc.module.settings;

public class StringSetting extends ModuleSetting<String> {

    public StringSetting(String defaultValue) {
        super(defaultValue);
    }

    @Override
    public String parse(String s) throws SettingParseException {
        return s;
    }
}
