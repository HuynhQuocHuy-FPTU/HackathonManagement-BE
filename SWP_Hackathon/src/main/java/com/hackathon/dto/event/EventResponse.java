package com.hackathon.dto.event;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.entity.Category;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Round;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.EventSeason;
import com.hackathon.entity.enums.WorkshopStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EventResponse {
    private Integer eventId;
    private String eventName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String title;
    private String address;
    private EventSeason season;
    private Integer seasonYear;
    private EventDescription description;
    private Integer maxTeam;
    private Integer minTeam;
    private Integer maxTeamSize;
    private Integer minTeamSize;
    private String bannerUrl;
    private LocalDateTime registrationDeadline;
    private LocalDateTime workshopTime;
    private WorkshopStatus workshopStatus;
    private EventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
    private List<CategoryResponse> categories;
    private List<RoundResponse> rounds;

    public EventResponse(HackathonEvent event, List<CategoryResponse> categories, List<RoundResponse> rounds){
        this.eventId = event.getEventId();
        this.eventName = event.getEventName();
        this.title = event.getTitle();
        this.season = event.getSeason();
        this.seasonYear = event.getSeasonYear();
        this.startDate = event.getStartDate();
        this.endDate = event.getEndDate();
        this.registrationDeadline = event.getRegistrationDeadline();
        this.address = event.getAddress();
        this.description = event.getDescription();
        this.maxTeam = event.getMaxTeam();
        this.minTeam = event.getMinTeam();
        this.maxTeamSize = event.getMaxTeamSize();
        this.minTeamSize = event.getMinTeamSize();
        this.status = event.getStatus();
        this.createdAt = event.getCreateAt();
        this.updateAt = event.getUpdateAt();
        this.bannerUrl = event.getBannerUrl();
        this.workshopStatus = event.getWorkshopStatus();
        this.workshopTime = event.getWorkshopTime();
        this.categories = categories;
        this.rounds = rounds;
    }

}
