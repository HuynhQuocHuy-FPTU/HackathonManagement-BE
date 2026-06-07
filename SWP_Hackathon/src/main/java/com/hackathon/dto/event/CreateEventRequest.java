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

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Max team is required")
    @Min(value = 1, message = "Max team must be at least 1")
    private Integer maxTeam;

    @NotNull(message = "Max team size is required")
    @Min(value = 1, message = "Max team size must be at least 1")
    private Integer maxTeamSize;

    @NotNull(message = "Min team size is required")
    @Min(value = 1, message = "Min team size must be at least 1")
    private Integer minTeamSize;

    @NotNull(message = "Registration deadline is required")
    private LocalDateTime registrationDeadline;

    private List<CreateCategoryRequest> categories;

    private List<CreateRoundRequest> rounds;
}
