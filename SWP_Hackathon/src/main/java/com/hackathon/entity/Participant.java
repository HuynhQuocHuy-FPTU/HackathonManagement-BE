package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.ParticipantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@Entity
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Disqualification_Reason")
    private String disqualificationReason;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @Column(name = "Total_Score")
    private double totalScore;

    @Column(name = "Rank")
    private Integer rank;

    @Column(name = "Present_Member")
    private Integer presentMembers;

    @OneToOne
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "CategoryRound_ID")
    private CategoryRound categoryRound;

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evaluation> evaluations = new ArrayList<>();

}
