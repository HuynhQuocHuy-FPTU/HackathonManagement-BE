package com.hackathon.entity;

import com.hackathon.entity.enums.NotiResponseStatus;
import com.hackathon.entity.enums.NotificationChannel;
import com.hackathon.entity.enums.InvitationStatus;
import com.hackathon.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "NVARCHAR(255)")
    private String title;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String message;
    @Column(name = "Is_Read")
    private boolean isRead;
    @Column(name = "Created_At")
    private LocalDateTime createdAt;


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;


    @Enumerated(EnumType.STRING)// moi bo sung
    // Check trạng thái của lời mời
    private InvitationStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Column(name = "Allow_Response")
    private boolean allowResponse;
    @Column(name = "Response_Deadline")
    private LocalDateTime responseDeadline;
    @Column(name = "Response_Message", columnDefinition = "NVARCHAR(MAX)")
    private String responseMessage;
    @Column(name = "Response_At")
    private LocalDateTime responseAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Response_Status")
    private NotiResponseStatus responseStatus;

    //N Notification - 1 Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_Id")
    private Account account;

    // người gửi / người thực hiện hành động
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Actor_ID")
    private Account actor;

    //N Notification - 1 Team
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Team_ID")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Round_ID")
    private Round round;

    // 1 team draft - n  noti
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Team_Draft_ID")
    private TeamDraft teamDraft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Team_Invitation_ID")
    private TeamInvitation teamInvitation;



}
