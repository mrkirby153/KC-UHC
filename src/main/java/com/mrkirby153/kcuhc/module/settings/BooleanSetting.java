package com.mrkirby153.kcuhc.module.settings;

import java.util.List;

public class BooleanSetting extends ModuleSetting<Boolean> {

    public BooleanSetting(Boolean defaultValue) {
        super(defaultValue);
    }

    @Override
    public Boolean parse(String s) {
        return Boolean.parseBoolean(s);
    }

    @Override
    public List<String> getCompletions(String input) {
        return List.of("true", "false");
    }
}
