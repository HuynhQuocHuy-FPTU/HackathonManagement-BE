package com.hackathon.dto.team;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.enums.RequestAction;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProcessTeamRequest {
    @NotNull(message = "Hành động xử lý không được để trống")
    private RequestAction action;

    private String responseMessage;

    private Integer eventId;
}
