package com.hackathon.service;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.repository.RoundRepository;
import com.hackathon.repository.TeamRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class StatusSchedulerService {

    private final RoundService roundService;
    private final HackathonEventRepository eventRepository;
    private final RoundRepository roundRepository;
    private final TeamRequestRepository teamRequestRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateEventStatusAuto() {
        List<EventStatus> excluded = List.of(
                EventStatus.DRAFT,
                EventStatus.COMPLETED,
                EventStatus.CANCELLED,
                EventStatus.DELETED
        );
        List<HackathonEvent> events = eventRepository.findAllActiveProcessingEvents(excluded);
        LocalDateTime now = LocalDateTime.now();

        for (HackathonEvent event : events) {
            EventStatus newStatus = resolveEventStatus(event, now);

            if (newStatus != null && event.getStatus() != newStatus) {
                event.setUpdateAt(LocalDateTime.now());
                event.setStatus(newStatus);
                if (newStatus == EventStatus.COMPLETED) {
                    for (Registration registration : event.getRegistrations()) {
                        Team team = registration.getTeam();
                        team.setStatus(TeamStatus.DRAFT);
                    }
                }
                eventRepository.save(event);
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateRoundStatusAuto() {
        List<EventStatus> eventStatuses = List.of(
                EventStatus.DRAFT,
                EventStatus.COMPLETED,
                EventStatus.CANCELLED,
                EventStatus.DELETED
        );
        List<Round> rounds = roundRepository.findRoundsOfActiveEvents(
                List.of(RoundStatus.COMPLETED),
                eventStatuses
        );
        LocalDateTime now = LocalDateTime.now();

        for (Round round : rounds) {
            RoundStatus newsStatus = this.resolveRoundStatus(round, now);

            if (newsStatus != round.getStatus()) {
                round.setStatus(newsStatus);
                roundService.saveRound(round);
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateWorkshopStatusAuto() {
        List<EventStatus> excluded = List.of(
                EventStatus.DRAFT,
                EventStatus.COMPLETED,
                EventStatus.CANCELLED,
                EventStatus.DELETED
        );
        List<HackathonEvent> events = eventRepository.findAllActiveProcessingEvents(excluded);
        LocalDateTime now = LocalDateTime.now();

        for (HackathonEvent event : events) {
            WorkshopStatus newStatus = calculateStatus(event, now); // Gọi hàm tính toán ở đây

            if (newStatus != null && event.getWorkshopStatus() != newStatus) {
                event.setWorkshopStatus(newStatus);
                eventRepository.save(event);
            }
        }
    }


    private EventStatus resolveEventStatus(HackathonEvent event, LocalDateTime now) {

        EventStatus currentStatus = event.getStatus();

        if (currentStatus == EventStatus.DRAFT || currentStatus == EventStatus.DELETED) {
            return null;
        }

        if (currentStatus == EventStatus.ACTIVE && !now.isBefore(event.getRegistrationDeadline())) {
            return EventStatus.REGISTRATION_CLOSED;
        }

        if (currentStatus == EventStatus.REGISTRATION_CLOSED && !now.isBefore(event.getStartDate())) {
            return EventStatus.ONGOING;
        }

        if (currentStatus == EventStatus.ONGOING && !now.isBefore(event.getEndDate())) {
            return EventStatus.COMPLETED;
        }
        return null;
    }

    private RoundStatus resolveRoundStatus(Round round, LocalDateTime now) {

        RoundStatus currentStatus = round.getStatus();
        // 1. Luồng tự động chuyển trạng thái SAU KHI HẾT HẠN PHÚC KHẢO
        if (currentStatus == RoundStatus.APPEALING && round.getAppealEndTime() != null) {
            if (now.isAfter(round.getAppealEndTime())) {
                return RoundStatus.PENDING_APPROVAL;
            }
            return RoundStatus.APPEALING;
        }

        // 2. Các trạng thái đặc biệt do Admin/Coordinator chủ động điều khiển (Giữ nguyên)
        if (currentStatus == RoundStatus.APPROVED ||
                currentStatus == RoundStatus.PENDING_APPROVAL ||
                currentStatus == RoundStatus.RE_EVALUATING ||
                currentStatus == RoundStatus.COMPLETED) {
            return currentStatus;
        }

        if (now.isBefore(round.getStartTime())) {
            return RoundStatus.UPCOMING;
        }

        if (now.isBefore(round.getSubmissionDeadline())) {
            return RoundStatus.ONGOING;
        }

        if (now.isBefore(round.getEndTime())) {
            return RoundStatus.EVALUATING;
        }


        return currentStatus;
//        return RoundStatus.COMPLETED;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkAndProcessExpiredAppeals() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Tìm các vòng thi đang ở trạng thái APPEALING và đã quá hạn phúc khảo ban đầu
        List<Round> expiredRounds = roundRepository.findByStatusAndAppealEndTimeBefore(
                RoundStatus.APPEALING, now
        );

        for (Round round : expiredRounds) {
            Integer roundId = round.getRoundId();

            // 2. Kiểm tra xem vòng này còn đơn phúc khảo nào chưa xử lý (PENDING hoặc IN_REVIEW) không
            List<TeamRequest> pendingOrInReviewRequests = teamRequestRepository
                    .findByRound_RoundIdAndRequestTypeAndStatusIn(
                            roundId,
                            RequestType.APPEAL,
                            List.of(RequestStatus.PENDING, RequestStatus.IN_REVIEW)
                    );

            if (pendingOrInReviewRequests != null && !pendingOrInReviewRequests.isEmpty()) {
                //  CHƯA XỬ LÝ XONG -> TỰ GIA HẠN 10 PHÚT
                if (now.isAfter(round.getAppealEndTime().plusMinutes(10))) {
                    // SAU 10P VẪN CH XƯR LÝ HỆ THÔNGS TỰ ĐỘNG TỪ CHỐI
                    for (TeamRequest req : pendingOrInReviewRequests) {
                        req.setStatus(RequestStatus.DECLINED);
                    }
                    teamRequestRepository.saveAll(pendingOrInReviewRequests);
                    round.setStatus(RoundStatus.PENDING_APPROVAL);
                    roundRepository.save(round);
                }

            } else {
                //  ĐÃ XỬ LÝ XONG XUÔI -> TỰ CHUYỂN TRẠNG THÁI
                round.setStatus(RoundStatus.PENDING_APPROVAL);
                roundRepository.save(round);

            }
        }
    }

    private WorkshopStatus calculateStatus(HackathonEvent event, LocalDateTime now) {
        if (event == null || event.getWorkshopTime() == null) return null;

        // 1. TRẠNG THÁI ĐÓNG BĂNG: Coordinator đã chốt (COMPLETED) hoặc đã bị hủy (CANCELLED)
        // Hệ thống tự động KHÔNG ĐƯỢC PHÉP can thiệp vào các trạng thái này.
        if (event.getWorkshopStatus() == WorkshopStatus.COMPLETED ||
                event.getWorkshopStatus() == WorkshopStatus.CANCELLED) {
            return event.getWorkshopStatus();
        }

        // 2. TRẠNG THÁI THỜI GIAN: Tính toán dựa trên thời gian thực
        LocalDateTime startTime = event.getWorkshopTime();

        if (now.isBefore(startTime)) {
            return WorkshopStatus.UPCOMING;
        }

        if (now.isBefore(startTime.plusHours(24))) {
            return WorkshopStatus.ONGOING;
        }

        // 3. MẶC ĐỊNH: Quá thời gian quy định (24h)
        return WorkshopStatus.COMPLETED;
    }


}
