package com.hackathon.dto.event;

import com.hackathon.dto.category.CreateCategoryRequest;
import com.hackathon.dto.category.UpdateCategoryRequest;
import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.UpdateRoundRequest;
import com.hackathon.entity.enums.EventSeason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class UpdateEventRequest {

    private String eventName;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String title;

    private String address;

    private EventDescription description;

    @Min(value = 1, message = "Số lượng đội phải lớn hơn hoặc bằng 1.")
    private Integer maxTeam;

    @Min(value = 1, message = "Số lượng đội tối thiểu phải lớn hơn hoặc bằng 1.")
    private Integer minTeam;

    @Min(value = 1, message = "Số lượng thành viên tối đa phải lớn hơn hoặc bằng 1.")
    private Integer maxTeamSize;

    @Min(value = 1, message = "Số lượng đội tối thiểu phải lớn hơn hoặc bằng 1.")
    private Integer minTeamSize;

    private String bannerUrl;

    private LocalDateTime workshopTime;

    private LocalDateTime registrationDeadline;

    @NotNull(message = "Season không được để trống")
    private EventSeason season;

    private List<UpdateCategoryRequest> categories;
    private List<UpdateRoundRequest> rounds;

}
