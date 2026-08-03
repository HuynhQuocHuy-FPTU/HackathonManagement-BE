package com.hackathon.dto.event;

import com.hackathon.dto.category.CreateCategoryRequest;
import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.entity.enums.EventSeason;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEventRequest {
    @NotBlank(message = "Tên cuộc thi là bắt buộc")
    private String eventName;
    @FutureOrPresent(message = "Ngày phải là thời điểm trong tương lai")
    private LocalDateTime startDate;
    @FutureOrPresent(message = "Ngày phải là thời điểm trong tương lai")
    private LocalDateTime endDate;

    private String title;

    private String address;

    private EventDescription description;

    @Min(value = 1, message = "Số lượng đội tối đa phải lớn hơn hoặc bằng 1.")
    private Integer maxTeam;

    @Min(value = 1, message = "Số lượng đội tối thiểu phải lớn hơn hoặc bằng 1.")
    private Integer minTeam;

    @Min(value = 1, message = "Số lượng thành viên tối đa phải lớn hơn hoặc bằng 1")
    private Integer maxTeamSize;

    @Min(value = 1, message = "Số lượng thành viên tối thiểu phải lớn hơn hoặc bằng 1")
    private Integer minTeamSize;

    private String bannerUrl;

    @FutureOrPresent(message = "Ngày phải là thời điểm trong tương lai")
    private LocalDateTime registrationDeadline;

    @FutureOrPresent(message = "Ngày phải là thời điểm trong tương lai")
    private LocalDateTime workshopTime;

    @NotNull(message = "Season không được để trống")
    private EventSeason season;

    private List<CreateCategoryRequest> categories;
    private List<CreateRoundRequest> rounds;


}
