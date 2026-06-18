package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.EventStatus;
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
public class HackathonEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Event_ID")
    private int eventId;
    @Column(name = "Event_Name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String eventName;
    @Column(name = "Start_Date", nullable = false)
    private LocalDateTime startDate;
    @Column(name = "End_Date", nullable = false)
    private LocalDateTime endDate;
    @Column(name = "Title", columnDefinition = "VARCHAR(255)", nullable = false)
    private String title;
    @Column(name = "Address", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String address;
    @Column(name = "Description", columnDefinition = "NVARCHAR(500)", nullable = false)
    private String description;
    @Column(name = "Season", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String season;
    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    @Column(name = "Max_Team")
    private Integer maxTeam;
    @Column(name = "Max_Team_Size", nullable = false)
    private Integer maxTeamSize;
    @Column(name = "Min_Team_Size", nullable = false)
    private Integer minTeamSize;
    @Column(name ="Registration_Dealine", nullable = false)
    private LocalDateTime registrationDeadline;
    @Column(name = "Create_At", nullable = false)
    private LocalDateTime createAt;
    @Column(name = "Update_At")
    private LocalDateTime updateAt;

    // 1 HACKATHON - N CATEGORY
    @OneToMany(mappedBy = "hackathonEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> categories = new ArrayList<>();

    // 1 EventCoordinator - N HackathonEvent
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Coordinator_ID", nullable = false)
    private EventCoordinator eventCoordinator;

    // 1 Hackthon - N round
    @OneToMany(mappedBy = "hackathonEvent", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Round> rounds = new ArrayList<>();

    //1 Hackthon - N Registration'
    @OneToMany(mappedBy = "hackathonEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Registration> registrations = new ArrayList<>();


}
