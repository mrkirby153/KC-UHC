package com.mrkirby153.kcuhc.module.settings;

public class IntegerSetting extends ModuleSetting<Integer> {

    public IntegerSetting(Integer defaultValue) {
        super(defaultValue);
    }

    @Override
    public Integer parse(String s) throws SettingParseException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new SettingParseException(e.getMessage());
        }
    }
}
