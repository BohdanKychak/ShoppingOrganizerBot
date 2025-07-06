package org.example.model.helper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.enums.Currency;


@Getter
@AllArgsConstructor
public class DualCurrencyTransaction {

    private Double convertibleAmount;

    private Currency convertibleCurrency;

    private Double convertedAmount;

    private Currency convertedCurrency;

}
