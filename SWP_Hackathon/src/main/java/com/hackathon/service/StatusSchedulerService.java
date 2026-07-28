package com.hackathon.service;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.repository.RoundRepository;
import com.hackathon.service.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.hackathon.entity.enums.RoundStatus.FINAL_RESULT;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusSchedulerService {

    private final RoundService roundService;
    private final HackathonEventRepository eventRepository;
    private final RoundRepository roundRepository;
    private final RankingService rankingService;
    private final RoundAdvancementService roundAdvancementService;
    private final NotificationService notificationService;


    @Scheduled(fixedRate = 30000)
    public void autoCalculateScores() {
        LocalDateTime now = LocalDateTime.now();
        List<Round> rounds = roundRepository.findByEvaluationDeadlineLessThanEqualAndScoringProcessedAtIsNull(now);

        for (Round round : rounds) {
            try {
                roundAdvancementService.calculateRoundScoresAutomatically(round.getRoundId());
                log.info("Đã tự động tính điểm cho round {}.", round.getRoundId());
                notifyCoordinatorsAboutScoringCompleted(round);
            } catch (Exception exception) {
                log.warn(
                        "Chưa thể tự động tính điểm cho round {}: {}",
                        round.getRoundId(),
                        exception.getMessage(),
                        exception
                );
                notifyCoordinatorAboutScoringFailure(round, exception);
            }
        }
    }

    private void notifyCoordinatorsAboutScoringCompleted(Round round) {
        try {
            notificationService.notifyScoringCompletedToAllCoordinators(round);
        } catch (Exception notificationException) {
            log.error(
                    "Đã tính điểm thành công nhưng không thể gửi thông báo cho tất cả Điều phối viên của vòng {}: {}",
                    round.getRoundId(),
                    notificationException.getMessage(),
                    notificationException
            );
        }
    }

    private void notifyCoordinatorAboutScoringFailure(Round round, Exception exception) {
        if (round.getScoringFailureNotifiedAt() != null) {
            return;
        }

        String reason = exception.getMessage() == null
                ? "Không xác định được nguyên nhân"
                : exception.getMessage();

        try {
            notificationService.notifyScoringFailureToAllCoordinators(round, reason);

            round.setScoringFailureNotifiedAt(LocalDateTime.now());
            roundRepository.save(round);
        } catch (Exception notificationException) {
            log.error(
                    "Không thể gửi thông báo lỗi tính điểm cho tất cả Điều phối viên của vòng {}: {}",
                    round.getRoundId(),
                    notificationException.getMessage(),
                    notificationException
            );
        }
    }

//    @Scheduled(fixedRate = 60000)
//    public void autoAdvanceRounds() {
//        LocalDateTime deadline = LocalDateTime.now().minusHours(1);
//        List<Round> rounds = roundRepository
//                .findByAppealEndTimeLessThanEqualAndAdvancementProcessedAtIsNull(deadline);
//
//        for (Round round : rounds) {
//            try {
//                roundAdvancementService.advanceRoundAutomatically(round.getRoundId());
//                log.info("Đã tự động thăng vòng cho round {}.", round.getRoundId());
//            } catch (Exception exception) {
//                // Giữ chưa xử lý để scheduler thử lại ở lần chạy sau.
//                log.error(
//                        "Không thể tự động thăng vòng cho round {}: {}",
//                        round.getRoundId(),
//                        exception.getMessage(),
//                        exception
//                );
//            }
//        }
//    }

    @Scheduled(fixedRate = 30000)
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

    @Scheduled(fixedRate = 30000)
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

    @Scheduled(fixedRate = 30000)
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

        if (currentStatus == RoundStatus.COMPLETED
                || !now.isBefore(round.getEndTime())) {
            return RoundStatus.COMPLETED;
        }

        if (currentStatus == FINAL_RESULT) {
            return RoundStatus.FINAL_RESULT;
        }


        if (now.isBefore(round.getStartTime())) {
            return RoundStatus.UPCOMING;
        }

        if (now.isBefore(round.getSubmissionDeadline())) {
            return RoundStatus.ONGOING;
        }

        if (now.isBefore(round.getEvaluationDeadline())) {
            return RoundStatus.EVALUATING;
        }

        // Sau khi chấm điểm, đội có thể gửi khiếu nại đến appealEndTime.
        if (round.getAppealEndTime() != null
                && now.isBefore(round.getAppealEndTime())) {
            return RoundStatus.APPEALING;
        }

        // Hết hạn gửi khiếu nại, chờ ban tổ chức xử lý.
        if (round.getResolveAppealDeadline() != null
                && now.isBefore(round.getResolveAppealDeadline())) {
            return RoundStatus.PENDING;
        }

        return RoundStatus.PENDING;
    }



//    @Scheduled(fixedRate = 60000)
////    @Transactional
//    public void autoManageRoundTimelines() {
//        LocalDateTime now = LocalDateTime.now();
//        List<RoundStatus> activeStatuses = List.of(
//                RoundStatus.ONGOING,
//                RoundStatus.UPCOMING,
//                RoundStatus.EVALUATING,
//                RoundStatus.PENDING,
//                RoundStatus.APPEALING
//        );
//
//        // Tim các vòng đang mở khiếu nại (APPEALING)
//        List<Round> activeAppealingRounds = roundRepository.findByStatusIn(activeStatuses);
//        log.info("Number of rounds found: {}", activeAppealingRounds.size());
//
//        for (Round round : activeAppealingRounds) {
//            try {
//                if (round.getResolveAppealDeadline() == null) {
//                    continue;
//                }
//                if (now.isAfter(round.getResolveAppealDeadline())) {
//                    log.info(
//                            "Round id={}, status={}, resolveDeadline={}",
//                            round.getRoundId(),
//                            round.getStatus(),
//                            round.getResolveAppealDeadline()
//                    );
//                    log.info(" Phát hiện vòng {} đã quá hạn giải quyết khiếu nại (Deadline: {}). Tiến hành chốt giải!",
//                            round.getRoundId(), round.getResolveAppealDeadline());
//
//                    rankingService.publishFinalRanking(round.getRoundId());
//                }
//            } catch (Exception e) {
//                log.error("Round {} failed: {}", round.getRoundId(), e.getMessage(), e);
//                log.error("Lỗi xảy ra khi tự động quét dòng thời gian của Vòng đấu {}: ", round.getRoundId(), e);
//            }
//        }
//
//    }


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
