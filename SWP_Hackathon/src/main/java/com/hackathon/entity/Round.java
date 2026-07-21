package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.FileType;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.entity.enums.SubmissionType;
import com.hackathon.service.submission.FileTypeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Round")
public class Round {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Round_ID")
    private Integer roundId;
    @Column(name = "Round_Name", columnDefinition = "NVARCHAR(255)", nullable = true)
    private String roundName;
    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;
    @Column(name = "Start_Time", nullable = true)
    private LocalDateTime startTime;
    @Column(name = "End_Time", nullable = true)
    private LocalDateTime endTime;
    @Column(name = "Advancement_Rule", nullable = true,columnDefinition = "NVARCHAR(MAX)")
    private String advancementRule;
    @Column(name = "Order_Index", nullable = true)
    private Integer orderIndex;
    @Column(name = "Appeal_StartTime", nullable = true)
    private LocalDateTime appealStartTime;
    @Column(name = "Appeal_EndTime", nullable = true)
    private LocalDateTime appealEndTime;

    @Column(name = "Advancement_Processed_At")
    private LocalDateTime advancementProcessedAt;

    @Column(name = "Scoring_Processed_At")
    private LocalDateTime scoringProcessedAt;

    @Column(name = "Scoring_Failure_Notified_At")
    private LocalDateTime scoringFailureNotifiedAt;

    @Column(name ="Url_Excels", columnDefinition = "NVARCHAR(MAX)")
    private String excelsUrl ;

    @Enumerated(EnumType.STRING)
    @Column(name = "Submission_Type", nullable = true)
    private SubmissionType submissionType;

    @Convert(converter = FileTypeConverter.class)
    @Column(name = "File_Type", columnDefinition = "NVARCHAR(255)", nullable = true)
    private List<FileType> allowedFileType;

    @Column(name = "Submission_Deadline", nullable = true)
    private LocalDateTime submissionDeadline;

    @Column(name = "Evaluation_Deadline", nullable = true)
    private LocalDateTime evaluationDeadline;

    @Column(name = "Resolve_Apeal_Deadline", nullable = true)
    private LocalDateTime resolveAppealDeadline;

    @Column(name = "Max_File_Count", nullable = true)
    private Integer maxFileCount;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private RoundStatus status;

    @Column(name = "Top_N", nullable = true)
    private Integer topN;



    // 1 round - N category_round
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryRound> categoryRounds = new ArrayList<>();

    // 1 hackathon - N round
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Event_ID", nullable = false)
    private HackathonEvent hackathonEvent;

    //1 Round - N Evaluation Criteria
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationCriteria> evaluationCriterias = new ArrayList<>();

    // 1 CriteriaSet - N round
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CriteriaSet_ID")
    private CriteriaSet criteriaSet;
}
