package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "CriteriaSet")
public class CriteriaSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CriteriaSet_ID")
    private int criteriaSetId;
    @Column(name = "CriteriaSet_Name", columnDefinition = "NVARCHAR(255)")
    private String criteriaSetName;
    @Column(name = "Max_Score", nullable = false)
    private Integer maxScore;

    // 1 eventCoordinator - N Criteria_SET
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Coordinator_ID", nullable = false)
    private EventCoordinator eventCoordinator;

    // 1 Criteria_set - N Criteria Detail
    @OneToMany(mappedBy = "criteriaSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CriteriaDetail> criteriaDetails = new ArrayList<>();

    //1 Criteria_Set - N round
    @OneToMany(mappedBy = "criteriaSet", cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonIgnore
    private List<Round> rounds = new ArrayList<>();



}
