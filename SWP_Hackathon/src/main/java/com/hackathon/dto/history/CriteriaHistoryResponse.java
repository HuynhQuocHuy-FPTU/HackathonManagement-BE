package com.hackathon.dto.history;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CriteriaHistoryResponse {
    private Integer accountId;
    private String accountName;
    private Integer criteriaSetId;
    private String criteriaSetName;
    List<EventInfo> eventInfo;

    @Getter
    @Setter
    public static class EventInfo{
        private Integer eventId;
        private String eventName;
    }



}
