package org.example.model.session.purchase;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.model.entity.Purchase;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class PurchaseSortedHistory {

    @Setter
    private LocalDateTime start;

    @Setter
    private LocalDateTime end;

    @Setter
    private Boolean activeUserState;

    @Setter
    private List<String> currencies;

    @Setter
    private BigDecimal amount1;

    @Setter
    private BigDecimal amount2;

    @Setter
    private InlineKeyboardMarkup keyboardMarkup;

    private List<Purchase> sortedPurchasesList;

    private Map<Long, Purchase> sortedPurchasesMap;

    @Setter
    private Long currentPurchaseId;

    public PurchaseSortedHistory(InlineKeyboardMarkup keyboardMarkup) {
        this.keyboardMarkup = keyboardMarkup;
    }

    public PurchaseSortedHistory(List<Purchase> sortedPurchasesList) {
        this.sortedPurchasesList = new ArrayList<>(sortedPurchasesList);
        this.sortedPurchasesMap = sortedPurchasesList.stream()
                .collect(Collectors.toMap(Purchase::getId, p -> p));
    }

    public Purchase getCurrentPurchase() {
        if (currentPurchaseId != null) {
            return sortedPurchasesMap.get(currentPurchaseId);
        }
        return null;
    }

}
