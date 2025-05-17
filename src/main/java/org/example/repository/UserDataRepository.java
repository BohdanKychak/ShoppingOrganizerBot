package org.example.repository;

import org.example.model.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserDataRepository extends JpaRepository<UserData, Long> {

    @Query("SELECT u FROM UserData u WHERE u.familyId = :familyId")
    List<UserData> findAllUsersByFamilyId(Long familyId);

    @Modifying
    @Transactional
    @Query("UPDATE UserData u SET u.locale = :locale WHERE u.chatId = :chatId")
    void updateLocaleByChatId(Long chatId, String locale);

    @Modifying
    @Transactional
    @Query("UPDATE UserData u SET u.familyId = :familyId WHERE u.chatId = :chatId")
    void updateFamilyIdByChatId(Long chatId, Long familyId);

}
