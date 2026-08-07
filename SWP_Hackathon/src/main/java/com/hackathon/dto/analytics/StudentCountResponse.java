package com.hackathon.dto.analytics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentCountResponse {
    private long fptStudentCount;
    private long externalStudentCount;
}