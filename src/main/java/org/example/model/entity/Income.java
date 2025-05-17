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
import org.example.model.session.UserSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Income")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    private String currency;

    private String description;

    @NotNull
    private LocalDateTime timeIncome;

    @NotNull
    private Long familyId;

    @NotNull
    private Long userId;

    public Income(Double amount, String currency, String description, UserSession session) {
        this.amount = new BigDecimal(amount);
        this.currency = currency;
        this.description = description;
        this.timeIncome = LocalDateTime.now();
        this.familyId = session.getFamilyId();
        this.userId = session.getChatId();
    }

    public Income(Double amount, String currency, UserSession session) {
        this.amount = new BigDecimal(amount);
        this.currency = currency;
        this.description = null;
        this.timeIncome = LocalDateTime.now();
        this.familyId = session.getFamilyId();
        this.userId = session.getChatId();
    }
}