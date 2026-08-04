package com.hackathon.entity;

import com.hackathon.entity.enums.TeamStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Team_Draft")
public class TeamDraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Team_Draft_ID" )
    private Long teamDraftId ;

    @Column(name="Team_Name", columnDefinition = "NVARCHAR(50)")
    private String teamName;

    @Column(name="Create_Date")
    @CreationTimestamp
    private LocalDateTime createAt;

    @Column(name = "Team_Size", nullable = false)
    private Integer teamSize;

    @Enumerated(EnumType.STRING)
    private TeamStatus status;

    // 1 Account (Leader) - N PendingTeam
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Account_ID", nullable = false)
    private Account account;

    // 1 PendingTeam - N Notification
    @OneToMany(mappedBy = "teamDraft", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();
}
