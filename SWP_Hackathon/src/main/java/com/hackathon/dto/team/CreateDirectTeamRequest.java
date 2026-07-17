package com.hackathon.dto.team;

import com.hackathon.entity.enums.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDirectTeamRequest {

    @NotNull(message = "Loại yêu cầu không được để trống")
    private RequestType requestType;

    @NotBlank(message = "Nội dung yêu cầu không được để trống")
    private String requestMessage;

    // Bắt buộc với APPEAL.
    private Integer roundId;

    // Bắt buộc với DRAW_RESULT_VERIFICATION.
    private Integer eventId;
}
