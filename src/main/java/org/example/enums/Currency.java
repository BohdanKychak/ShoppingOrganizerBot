package org.example.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.util.Constants.ACTIVE;
import static org.example.util.Constants.CURRENCY_LIST__FORMAT;
import static org.example.util.Constants.INACTIVE;
import static org.example.util.Constants.SEPARATOR;

@Getter
public enum Currency {
    // when adding new currencies -> need to update patterns
    // MONEY_TRANSACTION_PATTERN & MONEY_TRANSACTION_DESCRIPTION_PATTERN

    USD("USD", "American Dollar", "dollar", "$", "Dollar", "$"),
    EUR("EUR", "European Euro", "euro", "€", "Euro", "€"),
    CHF("CHF", "Schweizer Frank", "frank", "₣", "Fr", "Fr"),
    UAH("UAH", "Українська Гривня", "гривня", "₴", "Грн", "грн"),
    PLN("PLN", "Polski Złoty", "złoty", "zł", "Zloty", "zl"),
    ;

    private static final Map<String, Currency> CURRENCY_USED_MAP = new HashMap<>();
    private static final Map<String, Currency> CURRENCY_CODE_MAP = new HashMap<>();

    static {
        for (Currency currency : Currency.values()) {
            CURRENCY_USED_MAP.put(currency.used, currency);
            CURRENCY_CODE_MAP.put(currency.code, currency);
        }
    }

    private final String code;
    private final String fullName;
    private final String name;
    private final String symbol;
    private final String shortName;
    private final String used;

    Currency(String code, String fullName, String name, String symbol, String shortName, String used) {
        this.code = code;
        this.fullName = fullName;
        this.name = name;
        this.symbol = symbol;
        this.shortName = shortName;
        this.used = used;
    }

    public List<String> getCurrencyDescription() {
        return List.of(code, fullName, name, symbol, shortName, used);
    }

    public String getCurrencyDescriptionByFormat() {
        return String.format(CURRENCY_LIST__FORMAT, code, fullName, symbol, shortName);
    }

    public static Currency getCurrencyByStringUsed(String used) {
        return CURRENCY_USED_MAP.get(used);
    }

    public static Currency getCurrencyByStringCode(String code) {
        return CURRENCY_CODE_MAP.get(code);
    }

    public static String getCurrenciesDescriptionList() {
        return Arrays.stream(Currency.values())
                .map(Currency::getCurrencyDescriptionByFormat)
                .collect(Collectors.joining("\n"));
    }

    public static List<String> getCurrencyCodes() {
        return Arrays.stream(Currency.values())
                .map(Currency::getCode)
                .collect(Collectors.toList());
    }

    public static List<String> getCurrencyCodesWithStatus(List<String> codesToCheck) {
        if (codesToCheck == null) {
            codesToCheck = Collections.emptyList();
        }
        List<String> finalCodesToCheck = codesToCheck;

        return Arrays.stream(Currency.values())
                .map(currency -> currency.getCode()
                        + SEPARATOR
                        + (finalCodesToCheck.contains(currency.getCode()) ? ACTIVE : INACTIVE)
                )
                .collect(Collectors.toList());
    }
}
