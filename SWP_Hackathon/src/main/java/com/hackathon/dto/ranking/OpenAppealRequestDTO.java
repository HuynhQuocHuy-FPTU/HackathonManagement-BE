package com.hackathon.dto.ranking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OpenAppealRequestDTO {
    @NotNull(message = "ID của vòng thi không được để trống")
    private Integer roundId;
    @NotNull(message = "Thời gian mở cổng khiếu nại không được để trống")
    private LocalDateTime startTime;
    @NotNull(message = "Thời gian đóng cổng khiếu nại không được để trống")
    private LocalDateTime endTime;
}
