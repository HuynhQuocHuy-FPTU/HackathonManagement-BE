package com.hackathon.entity;

import com.hackathon.entity.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "TeamRequest")
public class TeamRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int requestId;
    @Column(name ="Create_Date")
    private LocalDateTime createDate;
    @Column(name = "Request_Status")
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    // 1 TEAM - N REQUEST
    @ManyToOne (fetch = FetchType.LAZY)
    private  Team team;

    // 1 EXPERT ASSIGN - N REQUEST
    @ManyToOne(fetch = FetchType.LAZY)
    private ExpertAssign expertAssign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_round_id")
    private CategoryRound categoryRound;


}
