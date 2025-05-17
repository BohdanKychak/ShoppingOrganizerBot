package org.example.repository;

import org.example.model.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("SELECT a FROM Account a WHERE a.familyId = :familyId")
    List<Account> findAccountsByFamilyId(Long familyId);

    @Query("SELECT a FROM Account a WHERE a.familyId = :familyId AND a.currency = :currency")
    Optional<Account> findAccountByFamilyIdAndCurrency(Long familyId, String currency);

    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.accountAmount = :accountAmount WHERE a.accountId = :accountId")
    void updateAmountById(Long accountId, BigDecimal accountAmount);

}
