package org.example.repository;

import org.example.model.entity.FamilyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FamilyDataRepository extends JpaRepository<FamilyData, Long> {

    @Query("SELECT f.familyId FROM FamilyData f")
    List<Long> findAllFamilyIds();

    @Query("SELECT f FROM FamilyData f WHERE f.familyId = :familyId")
    FamilyData findFamilyById(Long familyId);

    @Query("SELECT f FROM FamilyData f WHERE f.creatorId = :creatorId")
    List<FamilyData> findAllFamiliesByCreatorId(Long creatorId);

    // db saves set currencies like one string
    @Query("SELECT f.currency FROM FamilyData f WHERE f.familyId = :familyId")
    String findFamilyCurrenciesById(Long familyId);

    @Query("SELECT f FROM FamilyData f WHERE f.familyId = :familyId and f.passCode = :passCode")
    FamilyData findByFamilyIdAndPassCode(Long familyId, String passCode);

}
