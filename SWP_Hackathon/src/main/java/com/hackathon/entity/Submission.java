package com.hackathon.entity;

import com.hackathon.entity.enums.SubmissionStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="Submission")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Submission_ID")
    private int submission_id;
    @Column(name  ="Create_date")
    private LocalDateTime create_date;
    @Column(name = "Description", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String description;
    @Column(name="Github_URL")
    private String github_url;
    @Column(name="File_URL")
    private String file_url;
    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;


    // 1 Submission -N EVALUATION
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Evaluation>evaluations= new ArrayList<>();

    // 1 TEAM - N SUBMISSION
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Team_ID",nullable = false)
    private Team team;

    //1 categoryRound- N submission
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Category_Round_ID",nullable = false)
    private CategoryRound categoryRound;
}
