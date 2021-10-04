package com.mrkirby153.kcuhc.module.settings;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EnumSetting<T extends Enum<T>> extends ModuleSetting<T> {

    private Class<T> clazz;

    public EnumSetting(T defaultValue, Class<T> clazz) {
        super(defaultValue);
        this.clazz = clazz;
    }

    @Override
    public T parse(String s) throws SettingParseException {
        try {
            return Enum.valueOf(this.clazz, s.toUpperCase());
        } catch (Exception e) {
            throw new SettingParseException(e.getMessage());
        }
    }

    @Override
    public List<String> getCompletions(String input) {
        return Arrays.stream(clazz.getEnumConstants()).map(Objects::toString)
            .collect(Collectors.toList());
    }
}
