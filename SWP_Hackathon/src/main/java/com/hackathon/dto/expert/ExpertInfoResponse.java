package com.hackathon.dto.expert;

import com.hackathon.entity.enums.ExpertType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertInfoResponse {

    private int expertId;
    private String expertName;
    private String department;
    private ExpertType type;
    private String workplace;

}
