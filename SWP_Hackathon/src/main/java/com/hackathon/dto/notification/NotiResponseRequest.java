package com.hackathon.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NotiResponseRequest {
    @NotBlank
    private String message;

    // Bắt buộc khi phản hồi notification SUBMISSION_SCORED.
    private Integer roundId;
}
