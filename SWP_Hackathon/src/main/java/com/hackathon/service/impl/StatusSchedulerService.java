package com.hackathon.service.impl;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.repository.RegistrationRepository;
import com.hackathon.repository.RoundRepository;
import com.hackathon.repository.TeamRepository;
import com.hackathon.service.EventService;
import com.hackathon.service.NotificationService;
import com.hackathon.service.RankingService;
import com.hackathon.service.RoundService;
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
// Tự động cập nhật trạng thái sự kiện, workshop, vòng thi và kích hoạt các nghiệp vụ đúng thời điểm.
public class StatusSchedulerService {

    private final RoundService roundService;
    private final HackathonEventRepository eventRepository;
    private final RoundRepository roundRepository;
    private final RankingService rankingService;
    private final RoundAdvancementServiceImpl roundAdvancementServiceImpl;
    private final NotificationService notificationService;
    private final RegistrationRepository registrationRepository;
    private final EventService eventService;
    private final TeamRepository teamRepository;

    @Scheduled(fixedRate = 5000)
    // Tự động kết thúc các vòng đã qua thời gian và thực hiện bước chuyển vòng cần thiết.
    public void finalizeRoundsAtEndTime() {
        LocalDateTime now = LocalDateTime.now();
        List<Round> rounds = roundRepository.findByEndTimeLessThanEqualAndAdvancementProcessedAtIsNull(now);

        for (Round round : rounds) {
            try {
                roundAdvancementServiceImpl.calculateRoundScoresAutomatically(round.getRoundId());
                roundAdvancementServiceImpl.advanceRoundAutomatically(round.getRoundId());
                log.info(
                        "Đã tự động tính điểm, xếp hạng và thăng vòng cho round {} sau khi kết thúc.",
                        round.getRoundId()
                );
            } catch (Exception exception) {
                log.error(
                        "Chưa thể hoàn tất tự động round {} sau khi kết thúc: {}",
                        round.getRoundId(),
                        exception.getMessage(),
                        exception
                );
                notifyCoordinatorAboutScoringFailure(round, exception);
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    // Hủy các sự kiện không đạt số đội tối thiểu khi đã đến mốc kiểm tra.
    public void checkAndCancelEventsBelowMinimumTeams() {
        LocalDateTime now = LocalDateTime.now();
        List<HackathonEvent> events = eventRepository.findByStatusInAndRegistrationDeadlineLessThanEqual(List.of(EventStatus.REGISTRATION_CLOSED),now);

        for (HackathonEvent event : events) {
            Integer minTeam = event.getMinTeam();
            if (minTeam == null || minTeam < 1) {
                log.warn("Bỏ qua kiểm tra số đội tối thiểu của event {} vì minTeam không hợp lệ.", event.getEventId());
                continue;
            }

            long approvedTeamCount = registrationRepository.countByHackathonEvent_EventIdAndStatus(event.getEventId(), RegistrationStatus.APPROVED);

            if (approvedTeamCount >= minTeam) {
                continue;
            }

            String reason = "Không đủ số đội tối thiểu sau khi hết hạn đăng ký ("
                    + approvedTeamCount + "/" + minTeam + " đội).";
            try {
                eventService.cancelEventAutomatically(event.getEventId(), reason);
                log.info(
                        "Đã tự động hủy event {} vì chỉ có {}/{} đội được duyệt.",
                        event.getEventId(),
                        approvedTeamCount,
                        minTeam
                );
            } catch (Exception exception) {
                log.error(
                        "Không thể tự động hủy event {}: {}",
                        event.getEventId(),
                        exception.getMessage(),
                        exception
                );
            }

        }
    }



    @Scheduled(fixedRate = 5000)
    // Tự động tính điểm cho các vòng đã hết hạn chấm và chưa được xử lý.
    public void autoCalculateScores() {
        LocalDateTime now = LocalDateTime.now();
        List<Round> rounds = roundRepository.findByEvaluationDeadlineLessThanEqualAndScoringProcessedAtIsNull(now);

        for (Round round : rounds) {
            try {
                roundAdvancementServiceImpl.calculateRoundScoresAutomatically(round.getRoundId());
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

    // Gửi thông báo hoàn tất tính điểm đến ban tổ chức và đánh dấu đã thông báo.
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

    // Gửi cảnh báo tính điểm thất bại nhưng giới hạn tần suất để tránh thông báo lặp.
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

    @Scheduled(fixedRate = 5000)
    @Transactional
    // Duyệt các sự kiện và cập nhật trạng thái theo thời gian hiện tại.
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
                    List<Team> completedEventTeams = event.getRegistrations()
                            .stream()
                            .map(Registration::getTeam)
                            .distinct()
                            .peek(team -> team.setStatus(TeamStatus.ACTIVE))
                            .toList();
                    teamRepository.saveAll(completedEventTeams);
                }
                eventRepository.save(event);
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    // Duyệt các vòng thi và cập nhật trạng thái theo lịch đã cấu hình.
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

    @Scheduled(fixedRate = 5000)
    @Transactional
    // Cập nhật trạng thái workshop dựa trên thời gian và trạng thái sự kiện.
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


    // Xác định trạng thái sự kiện phù hợp với các mốc đăng ký, workshop và thi đấu.
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


        // Xác định trạng thái vòng dựa trên thời gian nộp bài, chấm điểm và khiếu nại.
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


    // Tính trạng thái workshop từ thời gian bắt đầu và trạng thái hoàn thành hoặc hủy hiện tại.
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
