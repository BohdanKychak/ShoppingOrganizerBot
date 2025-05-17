package org.example.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.action.tag.InlineButtonKey.AMOUNT_FILTER_BUTTON;
import static org.example.action.tag.InlineButtonKey.CURRENCY_FILTER_BUTTON;
import static org.example.action.tag.InlineButtonKey.DATE_FILTER_BUTTON;
import static org.example.action.tag.InlineButtonKey.USER_FILTER_BUTTON;

@Getter
public enum FilterCriteria {

    USER(USER_FILTER_BUTTON, 0, 0),
    AMOUNT(AMOUNT_FILTER_BUTTON, 0, 1),
    DATE(DATE_FILTER_BUTTON, 1, 0),
    CURRENCY(CURRENCY_FILTER_BUTTON, 1, 1);

    private final String code;
    private final int line;
    private final int column;

    FilterCriteria(String code, int line, int column) {
        this.code = code;
        this.line = line;
        this.column = column;
    }

    public static List<String> getFilterCriteriaCodes() {
        return Arrays.stream(FilterCriteria.values())
                .map(FilterCriteria::getCode)
                .collect(Collectors.toList());
    }
}
