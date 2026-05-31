package com.hackathon.entity;

import com.hackathon.entity.enums.TeamStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Team")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Team_ID" )
    private int team_id ;
    @Column(name="Team_Name", columnDefinition = "NVARCHAR(50)")
    private String team_name;
    @Column(name="Create_Date")
    @CreationTimestamp
    private LocalDateTime create_date;
    @Column(name="Status", columnDefinition = "VARCHAR(10)")
    @Enumerated(EnumType.STRING)
    private TeamStatus status;

    //N TeamMember - 1 Team
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> teamMembers = new ArrayList<>();

    //1 Team - N Registration
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Registration> registrations = new ArrayList<>();

    // 1 TEAM - N SUBMISSION
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL,orphanRemoval = true)
    private List< Submission> submissions = new ArrayList<>();
}
