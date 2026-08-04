package com.hackathon.dto.team;

import com.hackathon.entity.enums.InvitationStatus;
import lombok.*;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class TeamJoinResponse{
    private Long requestId;
    private Integer teamId;
    private String teamName;
    private Integer studentId;
    private String studentName;
    private String reason;
    private InvitationStatus status;
    private LocalDateTime createdAt;
}
