package org.example.model.session;

import lombok.Getter;
import lombok.Setter;
import org.example.model.session.purchase.PurchaseCreation;
import org.example.model.session.purchase.PurchaseLastHistory;
import org.example.model.session.purchase.PurchaseSortedHistory;

@Getter
public class TransactionSession {

    private PurchaseCreation purchaseCreation;

    @Setter
    private PurchaseLastHistory purchaseLastHistory;

    @Setter
    private PurchaseSortedHistory purchaseSortedHistory;

    private boolean wasNewTransactionAdded;

    public TransactionSession() {
        wasNewTransactionAdded = false;
    }

    public void setPurchaseCreation(PurchaseCreation purchaseCreation) {
        this.purchaseCreation = purchaseCreation;
        wasNewTransactionAdded = true;
    }

    public void tableUpdate() {
        wasNewTransactionAdded = true;
    }

    public void tableUpdated() {
        wasNewTransactionAdded = false;
    }

    public void clearPurchaseCreation() {
        purchaseCreation = null;
    }

    public void clearPurchaseLastHistory() {
        purchaseLastHistory = null;
    }

    public void clearPurchaseSortedHistory() {
        purchaseSortedHistory = null;
    }

    public void clearAll() {
        purchaseCreation = null;
        purchaseLastHistory = null;
        purchaseSortedHistory = null;
    }

}
