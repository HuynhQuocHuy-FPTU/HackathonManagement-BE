package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.dto.event.EventDescription;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.EventSeason;
import com.hackathon.entity.enums.WorkshopStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Column(name = "Start_Date", nullable = true)
    private LocalDateTime startDate;
    @Column(name = "End_Date", nullable = true)
    private LocalDateTime endDate;
    @Column(name = "Title", columnDefinition = "NVARCHAR(255)", nullable = true)
    private String title;
    @Column(name = "Address", columnDefinition = "NVARCHAR(255)", nullable = true)
    private String address;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)", nullable = true)
    private EventDescription description;
    @Enumerated(EnumType.STRING)
    @Column(name = "Season", nullable = true)
    private EventSeason season;
    @Column(name = "Season_Year", nullable = true)
    private Integer seasonYear;
    @Column(name = "Status", nullable = true)
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    @Column(name = "Max_Team", nullable = true)
    private Integer maxTeam;
    @Column(name = "Min_Team", nullable = true)
    private Integer minTeam;
    @Column(name = "Max_Team_Size", nullable = true)
    private Integer maxTeamSize;
    @Column(name = "Min_Team_Size", nullable = true)
    private Integer minTeamSize;
    @Column(name ="Registration_Dealine", nullable = true)
    private LocalDateTime registrationDeadline;
    @Column(name = "Create_At", nullable = false)
    private LocalDateTime createAt;
    @Column(name = "Update_At", nullable = true)
    private LocalDateTime updateAt;
    @Column(name = "Workshop_Time")
    private LocalDateTime workshopTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "Workshop_Status")
    private WorkshopStatus workshopStatus;
    @Column(name = "Banner_Url", nullable = true)
    private String bannerUrl;

    @Column(name = "Cancellation_Reason", nullable = true, columnDefinition = "NVARCHAR(255)")
    private String cancellationReason;

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
