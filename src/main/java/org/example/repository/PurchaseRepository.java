package org.example.repository;

import org.example.model.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    @Query("SELECT p FROM Purchase p WHERE p.familyId = :familyId ORDER BY p.timePurchase DESC LIMIT :limit")
    List<Purchase> getListPurchaseByFamilyIdSortByTimeDESCWithLimit(Long familyId, int limit);

    @Query("SELECT p FROM Purchase p WHERE p.familyId = :familyId ORDER BY p.timePurchase DESC LIMIT 1")
    Purchase findLatestPurchaseByFamilyId(Long familyId);

    @Modifying
    @Transactional
    @Query("UPDATE Purchase p SET p.description = :description, p.simpleDescription = :simpleDescription WHERE p.id = :id")
    void updateDescriptionById(Long id, String description, Boolean simpleDescription);

    @Modifying
    @Transactional
    @Query("UPDATE Purchase p SET p.receiptPhotoId = :receiptPhoto WHERE p.id = :id")
    void updateReceiptPhotoById(Long id, String receiptPhoto);

}
