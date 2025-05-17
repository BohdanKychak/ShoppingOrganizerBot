package org.example.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum Language {

    UKR("uk", "ukrainian"),
    ENG("en", "english"),
    ;

    private final String code;
    private final String name;

    Language(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static List<String> getLanguageCodes() {
        return Arrays.stream(Language.values())
                .map(Language::getCode)
                .collect(Collectors.toList());
    }
}
