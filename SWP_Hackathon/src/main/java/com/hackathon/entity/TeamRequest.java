package com.hackathon.entity;

import com.hackathon.entity.enums.NotiResponseStatus;
import com.hackathon.entity.enums.RequestStatus;
import com.hackathon.entity.enums.RequestType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

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
    @Column(name = "Create_Date")
    private LocalDateTime createDate;
    @Column(name = "Request_Status")
    @Enumerated(EnumType.STRING)
    private RequestStatus status;
    @Column(name = "Request_Message", columnDefinition = "NVARCHAR(MAX)")
    private String requestMessage;
    @Column(name = "Response_Message", columnDefinition = "NVARCHAR(MAX)")
    private String responseMessage;
    @Column(name = "Response_At")
    private LocalDateTime responseAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "Request_Type")
    private RequestType requestType;

    // người gửi / người thực hiện hành động
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Response_Id")
    private Account responder;


    // 1 TEAM - N REQUEST
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // 1 EXPERT ASSIGN - N REQUEST
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Assign_ID")
    private ExpertAssign expertAssign;

    // 1 ROUND - N REQUEST(BỔ SUNG)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Round_ID")
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Source_Notification_ID")
    private Notification sourceNotification;

    @Version
    private Integer version;


}
