package com.hackathon.dto.team;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathon.entity.enums.NotiResponseStatus;
import com.hackathon.entity.enums.RequestStatus;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamRequestResponse {
    private Integer requestId;
    private Integer teamId;
    private String teamName;
    private Integer expertId;
    private LocalDateTime createDate;
    private RequestStatus status;
    private String round;
    private String categoryName;
    private String requestMessage;
    private String responseMessage;
    private NotiResponseStatus responseStatus;
    private LocalDateTime responseAt;// thời gian phàn hồi của mentor


}
