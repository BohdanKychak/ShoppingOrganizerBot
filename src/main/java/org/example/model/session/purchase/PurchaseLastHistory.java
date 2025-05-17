package org.example.model.session.purchase;

import lombok.Getter;
import org.example.model.entity.Purchase;

public class PurchaseLastHistory {

    @Getter
    private Purchase purchase;

    @Getter
    private int index;

    private final int purchasesDataListSize;

    public PurchaseLastHistory(Purchase purchase, int index, int purchasesDataListSize) {
        this.purchase = purchase;
        this.index = index;
        this.purchasesDataListSize = purchasesDataListSize;
    }

    public void scroll(Purchase purchase, int index) {
        this.purchase = purchase;
        this.index = index;
    }

    public boolean hasNext() {
        return index + 1 != purchasesDataListSize;
    }

    public boolean hasPrevious() {
        return index != 0;
    }
}
