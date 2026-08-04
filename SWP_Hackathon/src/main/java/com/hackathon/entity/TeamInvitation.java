package com.hackathon.entity;

import com.hackathon.entity.enums.InvitationStatus;
import com.hackathon.entity.enums.InvitationType;
import com.hackathon.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Team_Invitation")
public class TeamInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Team_Invitation_ID" )
    private Long teamInvitationId ;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "Invitation_Type")
    private InvitationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false ,name = "Invitation_Status")
    private InvitationStatus status;

    private LocalDateTime createdAt;

    @Column(name = "Email", columnDefinition = "NVARCHAR(MAX)")
    private String email;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String reason;

    @ManyToOne
    @JoinColumn(name = "Team_ID", nullable = true)
    private Team team;

    @ManyToOne
    @JoinColumn(name = "Account_ID", nullable = true)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "Team_Draft_ID", nullable = true)
    private TeamDraft teamDraft;

}
