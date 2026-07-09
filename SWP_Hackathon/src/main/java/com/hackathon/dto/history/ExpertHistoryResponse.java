package com.hackathon.dto.history;

import com.hackathon.dto.event.EventDescription;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.entity.enums.ExpertType;
import com.hackathon.entity.enums.RegistrationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ExpertHistoryResponse {

    private Integer expertId;
    private String expertName;
    private String department;
    private ExpertType type;
    private List<ExpertHistoryDetail> histories;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class ExpertHistoryDetail{
        private Integer eventId;
        private String eventName;
        private String season;
        private Integer roundId;
        private Integer categoryId;
        private String roundName;

        private String categoryName;
        private ExpertRole expertRole;

    }

}
