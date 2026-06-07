package com.hackathon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Data
@Entity
@Table(name="CriteriaSet")
public class CriteriaSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CriteriaSet_ID")
    private int criteriaSetId;
    @Column(name = "Max_Score", precision = 10, scale = 2, nullable = false)
    private BigDecimal maxScore;

    // 1 eventCoordinator - N Criteria_SET
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Coordinator_ID", nullable = false)
    private EventCoordinator eventCoordinator;

    // 1 Criteria_set - N Criteria Detail
    @OneToMany(mappedBy = "criteriaSet", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<CriteriaDetail> criteriaDetails = new ArrayList<>();

//    //1 Criteria_Set - N round
//    @OneToMany(mappedBy = "criteriaSet", cascade = CascadeType.ALL,orphanRemoval = true)
//    private List<Round> rounds = new ArrayList<>();



}
