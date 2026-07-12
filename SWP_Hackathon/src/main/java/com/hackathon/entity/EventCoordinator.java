package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name ="EventCoordinator" )
public class EventCoordinator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Coordinator_ID")
    private int coordinatorId;
    @Column(name = "Coordinator_Name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String coordinatorName;
    @Column(name="Department")
    private String department;
    @Column(name = "Organization", columnDefinition = "NVARCHAR(255)")
    private String organization;

    // 1 ACCOUNT - 1 EVENT COORDINATOR
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="Account_ID", unique = true)
    private Account account;

    //1 EventCoordinator - N HackathonEvent
    @OneToMany(mappedBy = "eventCoordinator", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<HackathonEvent> hackathonEvents= new ArrayList<>();

    // 1 eventCoordinator - N Criteria_SET
    @OneToMany(mappedBy = "eventCoordinator", cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonIgnore
    private List<CriteriaSet>criteriaSets = new ArrayList<>();

}
