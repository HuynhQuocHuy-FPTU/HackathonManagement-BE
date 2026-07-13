package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.entity.enums.SubmissionStatus;
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

    @Column(name = "Submission_Status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus submissionStatus;

    @Column(name = "Disqualification_Reason", columnDefinition = "NVARCHAR(255)")
    private String disqualificationReason;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @Column(name = "Total_Score")
    private BigDecimal totalScore;

    @Column(name = "Rank")
    private Integer rank;

    @Column(name ="Award", columnDefinition = "NVARCHAR(MAX)")
    private String award;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Registration_Id")
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryRound_ID")
    private CategoryRound categoryRound;


    @OneToMany(mappedBy = "teamParticipant", cascade = CascadeType.ALL)
    private List<Submission> submissions = new ArrayList<>();

}
