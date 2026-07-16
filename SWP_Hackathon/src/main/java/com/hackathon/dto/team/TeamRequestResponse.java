package com.hackathon.dto.team;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathon.dto.evaluation.EvaluationResponse;
import com.hackathon.entity.enums.NotiResponseStatus;
import com.hackathon.entity.enums.RequestStatus;
import com.hackathon.entity.enums.RequestType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
    private RequestType requestType;
    private String categoryName;
    private String requestMessage;
    private String responseMessage;
    private Integer responseId;
    private LocalDateTime responseAt;// thời gian phàn hồi của mentor
    private List<EvaluationResponse> listEvaluation;
    private long acceptedRequests;
    private long rejectedRequests;
}
