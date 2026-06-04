package com.hackathon.entity;

import com.hackathon.entity.enums.TeamStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Registration")
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Registration_ID")
    private int registrationId;
    @Column(name = "Registration_Date")
    @CreationTimestamp
    private LocalDateTime registrationDate;
    @Column(name="Status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TeamStatus status;

    // 1 Category - N registration
    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "Category_ID", nullable = false)
    private Category category;

    //N Registration - 1 Team
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Team_ID", nullable = false)
    private Team team;

    // 1 HackathonEvent - N Registration
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Event_ID", nullable = false)
    private HackathonEvent hackathonEvent;
}
