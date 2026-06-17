package com.hackathon.dto.expert;

import com.hackathon.entity.enums.ExpertType;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ExpertInfoResponse {
    private int expertId;
    private String expertName;
    private String department;
    private ExpertType type;
    private String workplace;

}
