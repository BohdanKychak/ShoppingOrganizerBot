package org.example.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.session.purchase.PurchaseCreation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Purchase")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    private String currency;

    @Column(length = 200)
    private String description;

    private Boolean simpleDescription;

    private String receiptPhotoId;

    @NotNull
    private LocalDateTime timePurchase;

    @NotNull
    private Long familyId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "chatId", updatable = false)
    private UserData userData;

    public Purchase(PurchaseCreation purchaseCreation, Long familyId, Long chatId) {
        this.amount = purchaseCreation.getAmount();
        this.currency = purchaseCreation.getCurrency().getUsed();
        this.description = purchaseCreation.getDescription();
        this.simpleDescription = purchaseCreation.getSimpleDescription();
        this.receiptPhotoId = purchaseCreation.getReceiptPhotoId();
        this.timePurchase = LocalDateTime.now();
        this.familyId = familyId;
        this.userData = new UserData(chatId, null, null);
    }

    public UserData getUserData() {
        return userData == null ? new UserData() : userData;
    }
}