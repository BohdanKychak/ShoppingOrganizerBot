package org.example.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Family_Data")
public class FamilyData {

    @Id
    @NotNull
    private Long familyId;

    @NotNull
    private String passCode;

    @NotNull
    private String familyName;

    @NotNull
    private Set<Long> chatIds;

    @NotNull
    private Set<String> currency;

    @NotNull
    private Long creatorId;
}
