package com.hackathon.service.ranking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.dto.ParticipantResponseDTO;
import com.hackathon.dto.ranking.CategoryRankingResponse;
import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.OpenAppealRequestDTO;
import com.hackathon.dto.ranking.RankingResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.AuditService;

import com.hackathon.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {
    private final ParticipantRepository participantRepository;
    private final AuditService auditService;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final RoundRepository roundRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationRepository notificationRepository;
    private final EvaluationRepository evaluationRepository;
    private final TeamRequestRepository teamRequestRepository;
    private final NotificationService notificationService;

    //===============================================//
    //RANKING
    //===============================================//


    @Override
    public CategoryRoundRankingResponse getRankingByEventCoordinator(
            Integer roundId, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Event Coordinator."));

        Round round = roundRepository.findById(roundId).orElseThrow(
                () -> new BadRequestException("Không tìm thấy vòng thi này."));

        List<CategoryRankingResponse> categoriesRanking = new ArrayList<>();
        for (CategoryRound cr : round.getCategoryRounds()) {
            // Thông qua Category Round lấy các Team đnag tham gia cuộc thi
            List<TeamParticipant> teamParticipants = cr.getTeamParticipants();
            teamParticipants.sort(Comparator.comparing(TeamParticipant::getRank, Comparator.nullsLast(Integer::compareTo)));

            List<RankingResponseDTO> rankingResponse = new ArrayList<>();
            for (TeamParticipant participant : teamParticipants) {
                String teamName = (participant.getRegistration() != null) ? participant.getRegistration().getTeam().getTeamName() : "N/A";
                RankingResponseDTO dto = RankingResponseDTO.builder()
                        .participantId(participant.getId())
                        .totalScore(participant.getTotalScore())
                        .rank(participant.getRank())
                        .teamName(teamName)
                        .status(participant.getStatus())
                        .build();
                rankingResponse.add(dto);
            }
            CategoryRankingResponse response = CategoryRankingResponse.builder()
                    .categoryRoundId(cr.getCategoryRoundId())
                    .categoryId(cr.getCategory().getCategoryId())
                    .categoryName(cr.getCategory().getCategoryName())
                    .teams(rankingResponse)
                    .build();
            categoriesRanking.add(response);
        }


        return CategoryRoundRankingResponse.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .orderIndex(round.getOrderIndex())
                .advancementRule(round.getAdvancementRule())
                .topN(round.getTopN())
                .roundStatus(round.getStatus())
                .categoriesRanking(categoriesRanking).build();
    }

    private ParticipantResponseDTO mapToResponse(TeamParticipant teamParticipant) {
        if (teamParticipant == null) {
            return null;
        }
        String teamName = teamParticipant.getRegistration().getTeam().getTeamName();

        return ParticipantResponseDTO.builder()
                .participantId(teamParticipant.getId())
                .teamName(teamName)
                .totalScore(teamParticipant.getTotalScore())
                .rank(teamParticipant.getRank())
                .status(teamParticipant.getStatus())
                .build();
    }


    @Override
    @Transactional
    public CategoryRoundRankingResponse approveRanking(CustomUserDetails userDetails, Integer roundId) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Event Coordinator. Vì vậy bạn không được phép truy cập."));

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này"));
        // Chỉ xử lý khi vòng đấu đang ở EVALUATING
        if (round.getStatus() != RoundStatus.EVALUATING
                && round.getStatus() != RoundStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Vòng đấu đã kết thúc hoặc không trong trạng thái có thể phê duyệt.");
        }
        List<CategoryRound> categoryRound = round.getCategoryRounds();
        if (categoryRound == null || categoryRound.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này");
        }

        int topN = round.getTopN();
        // Kiểm tra trong tất cả hạng mục, các Team đã có điểm thi chưa
        List<CategoryRankingResponse> categoriesRanking = new ArrayList<>();
        for (CategoryRound cr : categoryRound) {
            List<RankingResponseDTO> rankingTeams = new ArrayList<>();
            // Lấy all team ngoại trừ những ng bị cấm thi or tự ý rời
            List<TeamParticipant> allTeams = cr.getTeamParticipants().stream()
                    .filter(t -> t.getStatus() != ParticipantStatus.DISQUALIFIED
                            && t.getStatus() != ParticipantStatus.WITHDRAWN)
                    .toList();

            // Check lấy đủ top N CHƯA
            long validTeams = allTeams.stream()
                    .filter(t -> t.getStatus() == ParticipantStatus.PASSED || t.getStatus() == ParticipantStatus.WINNER)
                    .count();
            if (validTeams < topN && allTeams.size() >= topN) {
                throw new BadRequestException(String.format(
                        "Không thể phê duyệt. Hạng mục '%s' chỉ mới chọn %d đội đi tiếp, không đủ số lượng chọn ra Top %d.",
                        cr.getCategory().getCategoryName(),
                        validTeams,
                        topN
                ));
            }

            for (TeamParticipant teamParticipant : cr.getTeamParticipants()) {
                if (teamParticipant.getStatus() == ParticipantStatus.ACTIVE) {
                    throw new BadRequestException("Không thể phê duyệt bảng xếp hạng vì vẫn còn đội chưa có kết quả cuối cùng (Trạng thái vẫn là ACTIVE).");
                }

                // Các đội không bị loại  vs rút lui bắt buộc phải có Rank và Score
                if (teamParticipant.getStatus() != ParticipantStatus.DISQUALIFIED && teamParticipant.getStatus() != ParticipantStatus.WITHDRAWN) {
                    if (teamParticipant.getRank() == null || teamParticipant.getTotalScore() == null) {
                        throw new BadRequestException(String.format(
                                "Đội '%s' ở hạng mục '%s' chưa được hệ thống tính điểm hoặc xếp hạng.",
                                teamParticipant.getRegistration().getTeam().getTeamName(),
                                cr.getCategory().getCategoryName()
                        ));
                    }
                }
                rankingTeams.add(
                        RankingResponseDTO.builder()
                                .participantId(teamParticipant.getId())
                                .teamName(teamParticipant.getRegistration().getTeam().getTeamName())
                                .totalScore(teamParticipant.getTotalScore())
                                .rank(teamParticipant.getRank())
                                .status(teamParticipant.getStatus())
                                .build());

            }
            categoriesRanking.add(
                    CategoryRankingResponse.builder()
                            .categoryRoundId(cr.getCategoryRoundId())
                            .categoryId(cr.getCategory().getCategoryId())
                            .categoryName(cr.getCategory().getCategoryName())
                            .teams(rankingTeams)
                            .build());

        }
        String logMessage;
        if (round.getStatus() == RoundStatus.PENDING_APPROVAL) {
            logMessage = "Phê duyệt lại kết quả cuối cùng sau phúc khảo thành công cho vòng: "+round.getRoundName();
        } else {
            logMessage = "Phê duyệt ranking lần 1 thành công của vòng: ";
        }
        round.setStatus(RoundStatus.APPROVED);
        roundRepository.save(round);

        auditService.saveLog(
                account,
                AuditAction.APPROVE_RANKING,
                AuditEntityType.ROUND,
                roundId,
                logMessage
        );

        return CategoryRoundRankingResponse.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .advancementRule(round.getAdvancementRule())
                .topN(round.getTopN())
                .roundStatus(round.getStatus())
                .categoriesRanking(categoriesRanking)
                .build();
    }

    @Override
    @Transactional
    public CategoryRoundRankingResponse rejectRanking(CustomUserDetails userDetails, Integer roundId) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Event Coordinator. Vì vậy bạn không được phép truy cập."));

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này"));

        List<CategoryRound> categoryRound = round.getCategoryRounds();
        if (categoryRound == null || categoryRound.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này");
        }

        RoundStatus currentStatus = round.getStatus();
        if (currentStatus != RoundStatus.EVALUATING &&
                currentStatus != RoundStatus.APPROVED &&
                currentStatus != RoundStatus.APPEALING) {
            throw new BadRequestException("Vòng đấu đã đóng hoặc kết thúc, không thể thực hiện thao tác từ chối.");
        }

        List<TeamParticipant> teamsToSave = new ArrayList<>();
        List<Evaluation> evaluationsToSave = new ArrayList<>();

        for (CategoryRound cr : categoryRound) {
            for (TeamParticipant team : cr.getTeamParticipants()) {
                if (team.getStatus() != ParticipantStatus.DISQUALIFIED && team.getStatus() != ParticipantStatus.WITHDRAWN) {
                    team.setRank(null);
                    team.setStatus(ParticipantStatus.ACTIVE);
                    teamsToSave.add(team);
                }


                // CHUYỂN VỀ TRẠNG THÁI RE_EVALUATION TIẾN HÀNH CHẤM ĐIỂM LẠI
                List<Evaluation> evaluations = evaluationRepository.findBySubmission_TeamParticipant(team);
                if (evaluations != null) {
                    for (Evaluation evaluation : evaluations) {
                        evaluation.setStatus(EvaluationStatus.RE_EVALUATION);
                        evaluationsToSave.add(evaluation);
                        team.setRank(null);

                    }
                }

            }
        }
        if (!evaluationsToSave.isEmpty()) {
            evaluationRepository.saveAll(evaluationsToSave);
            if (!teamsToSave.isEmpty()) {
                participantRepository.saveAll(teamsToSave);
            }
            round.setStatus(RoundStatus.RE_EVALUATING);

            roundRepository.save(round);

            auditService.saveLog(
                    account,
                    AuditAction.REJECT_RANKING,
                    AuditEntityType.ROUND,
                    roundId,
                    "Từ chối phê duyệt ranking thành công của vòng: " + round.getRoundName()
            );

        }
        return CategoryRoundRankingResponse.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .roundStatus(round.getStatus())
                .build();
    }

    @Override
    public void publishDraftRanking(Integer roundId, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức vì vậy bạn không có quyền truy cập vào dữ liệu này."));
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));
        if (round.getStatus() != RoundStatus.APPROVED) {
            throw new BadRequestException("Chỉ có thể công bố bảng xếp hạng tạm thời khi vòng thi đã được phê duyệt kết quả.");
        }

        List<CategoryRound> categoryRounds = round.getCategoryRounds();
        if (categoryRounds == null || categoryRounds.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này.");
        }
        boolean hasAprroved = round.getCategoryRounds().stream()
                .flatMap(categoryRound -> categoryRound.getTeamParticipants().stream())
                .allMatch(teamParticipant -> teamParticipant.getStatus() == ParticipantStatus.PASSED
                        || teamParticipant.getStatus() == ParticipantStatus.FAILED
                        || teamParticipant.getStatus() == ParticipantStatus.WITHDRAWN
                        || teamParticipant.getStatus() == ParticipantStatus.DISQUALIFIED
                        || teamParticipant.getStatus() == ParticipantStatus.WINNER);
        if (!hasAprroved) {
            throw new BadRequestException("Bạn cần phải duyệt bảng xếp hạng trước khi công bố kết quả tạm thời.");

        }
        // Công bố ranking nháp sau khi phê duyệt
        List<CategoryRankingResponse> auditRankingData = categoryRounds.stream().map(cr -> {
            List<RankingResponseDTO> rankingTeams = cr.getTeamParticipants().stream().map(tp ->
                    RankingResponseDTO.builder()
                            .participantId(tp.getId())
                            .teamName(tp.getRegistration().getTeam().getTeamName())
                            .totalScore(tp.getTotalScore())
                            .rank(tp.getRank())
                            .status(tp.getStatus())
                            .build()
            ).toList();

            return CategoryRankingResponse.builder()
                    .categoryRoundId(cr.getCategoryRoundId())
                    .categoryId(cr.getCategory().getCategoryId())
                    .categoryName(cr.getCategory().getCategoryName())
                    .teams(rankingTeams)
                    .build();
        }).toList();
        round.setStatus(RoundStatus.APPEALING);
        roundRepository.save(round);
        log.info("Đã công bố bản nháp bảng xếp hạng vòng {}. Bắt đầu nhận phúc khảo.", roundId);
        try {
            String jsonData = objectMapper.writeValueAsString(auditRankingData);

            auditService.saveLog(
                    account,
                    AuditAction.SAVE_DRAFT,
                    AuditEntityType.ROUND,
                    roundId,
                    jsonData
            );
        } catch (JsonProcessingException e) {
            log.error("Lỗi khi tuần tự hóa dữ liệu xếp hạng vòng {} sang JSON", roundId, e);
            throw new BadRequestException("Không thể lưu lịch sử bảng xếp hạng do lỗi hệ thống.");
        }
    }

    @Override
    public void publishFinalRanking(Integer roundId, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức vì vậy bạn không có quyền truy cập vào dữ liệu này."));

        // Check Round
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));
        if (round.getStatus() != RoundStatus.APPROVED ) {
            throw new BadRequestException("Vòng thi phải ở trạng thái chờ duyệt hoặc đang phúc khảo mới có thể công bố kết quả chính thức.");
        }

        if (round.getAppealEndTime() == null) {
            throw new BadRequestException("Vòng thi này chưa từng được thiết lập thời gian phúc khảo. Không thể công bố kết quả chính thức!");
        }
        // Công bố ranking chính thức sau khi phê duyệt và kết thúc thời gian phúc khảo

        List<CategoryRound> categoryRounds = round.getCategoryRounds();
        if (categoryRounds == null || categoryRounds.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này.");
        }

        if (LocalDateTime.now().isBefore(round.getAppealEndTime())) {
            throw new BadRequestException("Chưa hết thời gian phúc khảo vì vậy bạn không được phép công bố bản chính thức.");
        }

        for (CategoryRound cr : categoryRounds) {
            for (TeamParticipant tp : cr.getTeamParticipants()) {
                if (tp.getStatus() == ParticipantStatus.ACTIVE) {
                    throw new BadRequestException("Không thể công bố kết quả chính thức vì vẫn còn đội thi chưa được chấm điểm/xếp hạng (Trạng thái ACTIVE).");
                }
            }
        }
        List<TeamRequest> hasInReview = teamRequestRepository.findByRound_RoundIdAndRequestTypeAndStatus(roundId, RequestType.APPEAL, RequestStatus.IN_REVIEW);
        List<TeamRequest> hasPending = teamRequestRepository.findByRound_RoundIdAndRequestTypeAndStatus(roundId, RequestType.APPEAL, RequestStatus.PENDING);

        if (hasInReview != null && !hasInReview.isEmpty()) {
            throw new BadRequestException("Vẫn còn đơn phúc khảo đang được xử lý.");
        }

        if (hasPending != null && !hasPending.isEmpty()) {
            throw new BadRequestException("Vẫn còn đơn phúc khảo đang chờ duyệt (PENDING).");
        }
        List<CategoryRankingResponse> auditRankingData = categoryRounds.stream().map(cr -> {
            List<RankingResponseDTO> rankingTeams = cr.getTeamParticipants().stream().map(tp ->
                    RankingResponseDTO.builder()
                            .participantId(tp.getId())
                            .teamName(tp.getRegistration().getTeam().getTeamName())
                            .totalScore(tp.getTotalScore())
                            .rank(tp.getRank())
                            .status(tp.getStatus())
                            .build()
            ).toList();

            return CategoryRankingResponse.builder()
                    .categoryRoundId(cr.getCategoryRoundId())
                    .categoryId(cr.getCategory().getCategoryId())
                    .categoryName(cr.getCategory().getCategoryName())
                    .teams(rankingTeams)
                    .build();
        }).toList();
        round.setStatus(RoundStatus.COMPLETED);
        roundRepository.save(round);
        log.info("Đã công bố bản xếp hạng chính thức vòng {}. Đóng vòng đấu thành công!", roundId);

//        Notification notification = new Notification();
//        notification.setType(NotificationType.SYSTEM_ANNOUNCEMENT);
//        notification.setChannel(NotificationChannel.WEB);
//        notification.setTitle("KẾT QUẢ CUỘC THI.");
//        notification.setMessage("Ban tổ chức đã công bố kết quả chính thức của " + round.getRoundName());
//        notification.setCreatedAt(LocalDateTime.now());
//        notificationRepository.save(notification);
        notificationService.notifyRoundRankingPublished(account,roundId,true);


        try {
            String jsonData = objectMapper.writeValueAsString(auditRankingData);

            auditService.saveLog(
                    account,
                    AuditAction.PUBLISH_FINAL,
                    AuditEntityType.ROUND,
                    roundId,
                    jsonData
            );
        } catch (JsonProcessingException e) {
            log.error("Lỗi khi tuần tự hóa dữ liệu xếp hạng vòng {} sang JSON", roundId, e);
            throw new BadRequestException("Không thể lưu lịch sử bảng xếp hạng do lỗi hệ thống.");
        }

    }

    @Override
    public void openAppeals(CustomUserDetails userDetails, OpenAppealRequestDTO request) {
        Account account = userDetails.getAccount();
        EventCoordinator coordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức vì vậy bạn không có quyền truy cập vào dữ liệu này."));
        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng đấu mà bạn yêu cầu."));

        // Check thời gian được phép mở cổng khiếu naij
        // Sau khi đã công bố bảng tạm thời tiến hành mở cổng khiếu nại
        if (round.getStatus() != RoundStatus.APPEALING) {
            throw new BadRequestException("Chưa tới thời điểm mở cổng đăng ký khiếu nại kết quả cuộc thi" +
                    "Hệ thống chỉ cho phép mở cổng khiếu nại sau khi công bố kết quả tạm thời.");
        }

        if (round.getAppealStartTime() != null ||
                round.getAppealEndTime() != null) {
            throw new BadRequestException("Cổng khiếu nại đã được cấu hình trước đó.");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu.");
        }

        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(
                    "Thời gian mở cổng khiếu nại phải lớn hơn thời điểm hiện tại."
            );
        }
        if (request.getStartTime().isBefore(round.getStartTime()) || request.getEndTime().isAfter(round.getEndTime())) {
            throw new BadRequestException("Thời gian bắt đầu và kết thúc của cổng đăng ký khiếu nại phải nằm trong thời gian của vòng đấu");

        }

        // Tiến hành mở cổng khiếu nại(Sau khi công bố Draf)

        round.setAppealStartTime(request.getStartTime());
        round.setAppealEndTime(request.getEndTime());
        roundRepository.save(round);
        auditService.saveLog(
                account,
                AuditAction.OPEN_APPEAL,
                AuditEntityType.ROUND,
                request.getRoundId(),
                "Mở cộng khiếu nại phúc khảo thành công của vòng: " + round.getRoundName()
        );

    }

}
