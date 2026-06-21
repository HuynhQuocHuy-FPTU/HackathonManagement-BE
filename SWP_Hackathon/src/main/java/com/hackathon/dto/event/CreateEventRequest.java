package com.hackathon.dto.event;

import com.hackathon.dto.category.CreateCategoryRequest;
import com.hackathon.dto.round.CreateRoundRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEventRequest {
    @NotBlank(message = "Event name is required")
    private String eventName;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String title;

    private String address;

    private String description;

    @Min(value = 1, message = "Max team must be at least 1")
    private Integer maxTeam;

    @Min(value = 1, message = "Max team size must be at least 1")
    private Integer maxTeamSize;

    @Min(value = 1, message = "Min team size must be at least 1")
    private Integer minTeamSize;

    private LocalDateTime registrationDeadline;

    private List<CreateCategoryRequest> categories;
    private List<CreateRoundRequest> rounds;

}
