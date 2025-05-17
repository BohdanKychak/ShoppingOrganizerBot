package org.example.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "User_Data")
public class UserData {

    @Id
    @NotNull
    private Long chatId;

    private String userName;

    private Long familyId;

    private String locale;

    public UserData(Long chatId, String userName, String locale) {
        this.chatId = chatId;
        this.userName = userName;
        this.locale = locale;
    }
}