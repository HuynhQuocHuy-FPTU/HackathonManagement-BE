package com.hackathon.entity;

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

    @Column(name = "Submission_Status", nullable = true)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus submissionStatus;

    @Column(name = "Disqualification_Reason", columnDefinition = "NVARCHAR(255)", nullable = true)
    private String disqualificationReason;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @Column(name = "Total_Score", nullable = true)
    private BigDecimal totalScore;

    @Column(name = "Rank", nullable = true)
    private Integer rank;

    @Column(name ="Title_Award", columnDefinition = "NVARCHAR(MAX)")
    private String titleAward;

    @Column(name ="Award", columnDefinition = "NVARCHAR(MAX)")
    private String award;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Registration_Id")
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryRound_ID", nullable = true)
    private CategoryRound categoryRound;


    @OneToMany(mappedBy = "teamParticipant", cascade = CascadeType.ALL)
    private List<Submission> submissions = new ArrayList<>();

}
