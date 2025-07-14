package org.example.model.session.purchase;

import lombok.Getter;
import org.example.enums.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class PurchaseCreation {

    private final BigDecimal amount;

    private final Currency currency;

    private final BigDecimal secondAmount;

    private final Currency secondCurrency;

    private String description;

    private Boolean simpleDescription;

    private Boolean hasDescription;

    private String receiptPhotoId;

    public PurchaseCreation(Double amount, Currency currency) {
        this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
        this.secondAmount = null;
        this.secondCurrency = null;
        this.hasDescription = null;
    }

    public PurchaseCreation(Double amount1, Currency currency1, Double amount2, Currency currency2) {
        this.amount = new BigDecimal(amount1).setScale(2, RoundingMode.HALF_UP);
        this.currency = currency1;
        this.secondAmount = new BigDecimal(amount2).setScale(2, RoundingMode.HALF_UP);
        this.secondCurrency = currency2;
        this.hasDescription = null;
    }

    public void addDescription(String description, boolean simpleDescription) {
        this.description = description;
        this.simpleDescription = simpleDescription;
        this.hasDescription = true;
    }

    public void skipDescription() {
        this.simpleDescription = false;
        this.hasDescription = false;
    }

    public void addReceiptPhotoId(String receiptPhotoId) {
        this.receiptPhotoId = receiptPhotoId;
    }
}
