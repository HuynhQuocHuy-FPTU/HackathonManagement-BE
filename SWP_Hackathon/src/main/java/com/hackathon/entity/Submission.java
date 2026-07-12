package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name="Submission")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Submission_ID")
    private int submissionId;
    @Column(name  ="Create_date")
    private LocalDateTime createAt;
    @Column(name = "Description", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String description;
    @Column(name="Github_URL")
    private String githubUrl;

    @Column(name = "Is_Final")
    private boolean isFinal;


    // 1 Submission -N EVALUATION
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Evaluation>evaluations= new ArrayList<>();

    // 1 TEAM - N SUBMISSION
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Team_ID",nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Team_Participant_Id", nullable = false)
    private TeamParticipant teamParticipant;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<SubmissionFile> files;

//    //1 categoryRound- N submission
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JsonIgnore
//    @JoinColumn(name = "Category_Round_ID",nullable = false)
//    private CategoryRound categoryRound;
}
