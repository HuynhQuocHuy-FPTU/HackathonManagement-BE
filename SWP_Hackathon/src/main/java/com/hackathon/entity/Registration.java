package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.entity.enums.TeamStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
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
    private RegistrationStatus status;

    //N Registration - 1 Team
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Team_ID", nullable = false)
    private Team team;

    // 1 HackathonEvent - N Registration
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Event_ID", nullable = false)
    private HackathonEvent hackathonEvent;

    @OneToOne
    private Participant participant;


}
