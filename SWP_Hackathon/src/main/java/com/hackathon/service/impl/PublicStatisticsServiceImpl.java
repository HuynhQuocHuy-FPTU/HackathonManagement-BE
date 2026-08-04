package com.hackathon.service.impl;

import com.hackathon.dto.PublicStatisticsResponse;
import com.hackathon.entity.Team;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.repository.RegistrationRepository;
import com.hackathon.service.PublicStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicStatisticsServiceImpl implements PublicStatisticsService {

    private static final List<EventStatus> EXCLUDED_EVENT_STATUSES = List.of(
            EventStatus.DRAFT,
            EventStatus.DELETED,
            EventStatus.CANCELLED
    );

    private final HackathonEventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    @Transactional(readOnly = true)
    public PublicStatisticsResponse getPublicStatistics() {
        long eventCount =
                eventRepository.countByStatusNotIn(EXCLUDED_EVENT_STATUSES);
        List<Team> approvedTeams = registrationRepository
                .findDistinctTeamsByRegistrationStatusAndEventStatusNotIn(
                        RegistrationStatus.APPROVED,
                        EXCLUDED_EVENT_STATUSES
                );
        long teamCount = approvedTeams.size();
        long participantCount = approvedTeams.stream()
                .map(Team::getTeamSize)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();

        return PublicStatisticsResponse.builder()
                .eventCount(eventCount)
                .participantCount(participantCount)
                .teamCount(teamCount)
                .build();
    }
}
