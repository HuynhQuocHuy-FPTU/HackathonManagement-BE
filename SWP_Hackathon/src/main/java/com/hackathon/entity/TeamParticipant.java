package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.ParticipantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@Entity
public class TeamParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Disqualification_Reason", columnDefinition = "NVARCHAR(255)")
    private String disqualificationReason;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @Column(name = "Total_Score")
//    private double totalScore;
    private BigDecimal totalScore;
    @Column(name = "Rank")
    private Integer rank;

    @OneToOne
    @JoinColumn(name = "Registration_Id", nullable = false, unique = true)
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryRound_ID")
    private CategoryRound categoryRound;

    @OneToMany(mappedBy = "teamParticipant", cascade = CascadeType.ALL)
    private List<Evaluation> evaluations = new ArrayList<>();

}
