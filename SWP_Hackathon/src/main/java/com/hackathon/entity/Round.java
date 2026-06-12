package com.hackathon.entity;

import com.hackathon.entity.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "Round")
public class Round {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Round_ID")
    private int roundId;
    @Column(name = "Round_Name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String roundName;
    @Column(name = "Start_Time", nullable = false)
    private LocalDateTime startTime;
    @Column(name = "End_Time", nullable = false)
    private LocalDateTime endTime;
    @Column(name = "Advancement_Rule", nullable = false)
    private String advancementRule;
    @Column(name = "Order_Index", nullable = false)
    private Integer orderIndex;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoundStatus status;

    // 1 round - N category_round
    @OneToMany(mappedBy = "round")
    private List<CategoryRound> categoryRounds = new ArrayList<>();

    // 1 hackathon - N round
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Event_ID", nullable = false)
    private HackathonEvent hackathonEvent;

    //1 Round - N Evaluation Criteria
    @OneToMany(mappedBy = "round")
    private List<EvaluationCriteria> evaluationCriterias = new ArrayList<>();

    // 1 CriteriaSet - N round
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CriteriaSet_ID", nullable = false)
    private CriteriaSet criteriaSet;



}
