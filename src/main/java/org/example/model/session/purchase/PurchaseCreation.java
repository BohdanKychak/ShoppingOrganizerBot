package org.example.model.session.purchase;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class PurchaseCreation {

    private final BigDecimal amount;

    private final String currency;

    private String description;

    private Boolean simpleDescription;

    private Boolean hasDescription;

    private String receiptPhotoId;

    public PurchaseCreation(Double amount, String currency) {
        this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
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
