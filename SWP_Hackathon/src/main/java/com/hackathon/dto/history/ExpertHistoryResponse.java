package com.hackathon.dto.history;

import com.hackathon.entity.enums.ExpertType;
import com.hackathon.entity.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ExpertHistoryResponse {
    private String eventName;
    private String expertName;
    private String department;
    private ExpertType type;
    private List<Map<String, Object>> histories;

}
