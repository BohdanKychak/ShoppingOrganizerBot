package org.example.repository;

import org.example.model.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    @Query("SELECT i FROM Income i WHERE familyId = :familyId ORDER BY timeIncome DESC LIMIT :limit")
    List<Income> getListIncomeByFamilyIdSortByTimeDESCWithLimit(Long familyId, int limit);

    @Modifying
    @Transactional
    @Query("UPDATE Income i SET i.description = :description WHERE i.id = :id")
    void updateDescriptionById(Long id, String description);
}
