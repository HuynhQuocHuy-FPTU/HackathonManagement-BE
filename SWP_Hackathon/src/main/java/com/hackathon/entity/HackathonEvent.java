package com.hackathon.entity;

import com.hackathon.entity.enums.EventStatus;
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
public class HackathonEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Event_ID")
    private int event_id;
    @Column(name = "Event_Name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String event_name;
    @Column(name = "Start_Date")
    private LocalDateTime start_date;
    @Column(name = "End_Date")
    private LocalDateTime end_date;
    @Column(name = "Title", columnDefinition = "VARCHAR(50)", nullable = false)
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
    // 1 HACKATHON - N CATEGORY
    @OneToMany(mappedBy = "hackathonEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> categorys = new ArrayList<>();

    // 1 EventCoordinator - N HackathonEvent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Coordinator_ID", nullable = false)
    private EventCoordinator eventCoordinator;

    // 1 Hackthon - N round
    @OneToMany(mappedBy = "hackathonEvent", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Round> rounds = new ArrayList<>();

    //1 Hackthon - N Registration'
    @OneToMany(mappedBy = "hackathonEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Registration> registrations = new ArrayList<>();

}
