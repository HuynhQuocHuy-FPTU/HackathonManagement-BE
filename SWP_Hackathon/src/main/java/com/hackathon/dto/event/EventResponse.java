package com.hackathon.dto.event;

import com.hackathon.dto.category.CategoryResponse;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.entity.Category;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Round;
import com.hackathon.entity.enums.EventStatus;
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
    private String season;
    private String description;
    private Integer maxTeam;
    private Integer maxTeamSize;
    private Integer minTeamSize;
    private LocalDateTime registrationDeadline;
    private EventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
    private String bannerUrl;
    private List<CategoryResponse> categories;
    private List<RoundResponse> rounds;

    public EventResponse(HackathonEvent event, List<CategoryResponse> categories, List<RoundResponse> rounds){
        this.eventId = event.getEventId();
        this.eventName = event.getEventName();
        this.title = event.getTitle();
        this.season = event.getSeason();
        this.startDate = event.getStartDate();
        this.endDate = event.getEndDate();
        this.registrationDeadline = event.getRegistrationDeadline();
        this.address = event.getAddress();
        this.description = event.getDescription();
        this.maxTeam = event.getMaxTeam();
        this.maxTeamSize = event.getMaxTeamSize();
        this.minTeamSize = event.getMinTeamSize();
        this.status = event.getStatus();
        this.createdAt = event.getCreateAt();
        this.updateAt = event.getUpdateAt();
        this.bannerUrl = event.getBannerUrl();

        this.categories = categories;
        this.rounds = rounds;
    }

}
