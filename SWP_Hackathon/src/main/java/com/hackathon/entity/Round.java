package com.hackathon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@Builder
@Entity
@Table(name = "Round")
public class Round {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Round_ID")
    private int round_id;
    @Column(name="Round_Name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String roundName;
    @Column(name="Start_Time", nullable = false)
    private LocalDateTime startTime;
    @Column(name="End_Time", nullable = false)
    private LocalDateTime endTime;
    @Column(name = "Advancement_Rule", nullable = false)
    private String advancementRule;

    // 1 round - N category_round
    @OneToMany(mappedBy = "round")
    private List<CategoryRound> categoryRounds = new ArrayList<>();

    // 1 hackathon - N round
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Event_ID", nullable = false)
    private HackathonEvent hackathonEvent;

    //1 Criteria_Set - N round
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CriteriaSet_ID", nullable = false)
    private CriteriaSet criteriaSet;


}
