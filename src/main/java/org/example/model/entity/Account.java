package org.example.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.Currency;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long accountId;

    @NotNull
    private String currency;

    @NotNull
    private String currencyName;

    @NotNull
    private String currencyCode;

    @NotNull
    @Column(precision = 12, scale = 2)
    private BigDecimal accountAmount;

    @NotNull
    private Long familyId;

    public Account(Currency currency, Double accountAmount, Long familyId) {
        this.currency = currency.getUsed();
        this.currencyName = currency.getName();
        this.currencyCode = currency.getCode();
        this.accountAmount = new BigDecimal(accountAmount);
        this.familyId = familyId;
    }
}
