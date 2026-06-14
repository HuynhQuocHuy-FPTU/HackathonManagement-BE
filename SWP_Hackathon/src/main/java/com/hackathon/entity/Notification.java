package com.hackathon.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "NVARCHAR(255)")
    private String title;
    @Column(columnDefinition = "NVARCHAR(1000)")
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    //N Notification - 1 Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_Id", nullable = false )
    private Account account;

    //N Notification - 1 Team
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Team_ID")
    private Team team;
    //N Notification - 1 event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Event_ID")
    private HackathonEvent event;

    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
        isRead = false;
    }

}
