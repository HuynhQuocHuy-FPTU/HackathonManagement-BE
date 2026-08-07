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
// Tổng hợp các số liệu công khai về sự kiện, đội thi và người tham gia.
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
    // Tính số sự kiện được công khai cùng số đội và thành viên đã được duyệt tham gia.
    public PublicStatisticsResponse getPublicStatistics() {
        // Không tính sự kiện nháp, đã xóa hoặc đã hủy vào số liệu công khai.
        long eventCount =
                eventRepository.countByStatusNotIn(EXCLUDED_EVENT_STATUSES);
        // Lấy các đội khác nhau có ít nhất một đăng ký được duyệt trong sự kiện hợp lệ.
        List<Team> approvedTeams = registrationRepository
                .findDistinctTeamsByRegistrationStatusAndEventStatusNotIn(
                        RegistrationStatus.APPROVED,
                        EXCLUDED_EVENT_STATUSES
                );
        // Số đội bằng kích thước danh sách đã loại bỏ trùng lặp.
        long teamCount = approvedTeams.size();
        // Tổng số người tham gia bằng tổng quy mô của các đội được duyệt.
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
