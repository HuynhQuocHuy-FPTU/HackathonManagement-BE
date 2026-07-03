package com.hackathon.service.ranking;

import com.hackathon.dto.ParticipantResponseDTO;
import com.hackathon.dto.ranking.CategoryRankingResponse;
import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.OpenAppealRequestDTO;
import com.hackathon.dto.ranking.RankingResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.AuditService;
import com.hackathon.service.NotificationService;
import com.hackathon.validator.DisqualifyValidator;
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
    private final ExpertRepository expertRepository;
    private final ExpertAssignRepository expertAssignRepository;
    private final ParticipantRepository participantRepository;
    private final TeamRepository teamRepository;
    private final DisqualifyValidator disqualifyValidator;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final HackathonEventRepository hackathonEventRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final RoundRepository roundRepository;


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
        if (round.getStatus() != RoundStatus.EVALUATING) {
            throw new BadRequestException("Vòng đấu đã kết thúc hoặc không trong trạng thái có thể phê duyệt.");
        }
        List<CategoryRound> categoryRound = round.getCategoryRounds();
        if (categoryRound == null || categoryRound.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này");
        }

        int topN = round.getTopN();
        int totalTeams = 0;
        int totalPassed = 0;
        int totalFailed = 0;
        // Kiểm tra trong tất cả hạng mục, các Team đã có điểm thi chưa
        List<ParticipantResponseDTO> teamsResultList = new ArrayList<>();
        for (CategoryRound cr : categoryRound) {
            // Check lấy đủ top N CHƯA
            long validTeams = cr.getTeamParticipants().stream()
                    .filter(t -> t.getStatus() == ParticipantStatus.ACTIVE
                            || t.getStatus() == ParticipantStatus.PASSED)
                    .count();

            if (validTeams < topN) {
                log.info("Category {} chỉ có {} team hợp lệ < topN {}",
                        cr.getCategory().getCategoryName(),
                        validTeams,
                        topN);
                throw new BadRequestException(String.format(
                        "Không thể phê duyệt. Hạng mục '%s' chỉ có %d đội hợp lệ, không đủ số lượng chọn ra Top %d.",
                        cr.getCategory().getCategoryName(),
                        validTeams,
                        topN
                ));
            }

            for (TeamParticipant teamParticipant : cr.getTeamParticipants()) {
                // Check team bị loại mà vẫn có trong ds ranking
                if (teamParticipant.getStatus() == ParticipantStatus.DISQUALIFIED
                        || teamParticipant.getStatus() == ParticipantStatus.FAILED
                        || teamParticipant.getStatus() == ParticipantStatus.WITHDRAWN) {
                    teamsResultList.add(mapToResponse(teamParticipant));
                    continue;
                }

                // Check ban giám khảo đã chấm điểm hết chưa
                if (teamParticipant.getEvaluations() == null || teamParticipant.getEvaluations().isEmpty()) {
                    throw new BadRequestException(String.format("Không thể phê duyệt. Đội '%s' chưa có dữ liệu đánh giá ở hạng mục '%s'."
                            , teamParticipant.getRegistration().getTeam().getTeamName()
                            , cr.getCategory().getCategoryName()));
                }
                for (Evaluation evaluation : teamParticipant.getEvaluations()) {
                    if (evaluation.getStatus() == EvaluationStatus.NOT_GRADED) {
                        throw new BadRequestException("Ban giám khảo chưa thực hiện xong quá trình chấm điểm");
                    }
                }
                if (teamParticipant.getRank() != null && teamParticipant.getRank() <= topN) {
                    teamParticipant.setStatus(ParticipantStatus.PASSED);
                    totalPassed++;
                } else {
                    teamParticipant.setStatus(ParticipantStatus.FAILED);
                    totalFailed++;
                }
                totalTeams++;
                TeamParticipant savedTeam = participantRepository.save(teamParticipant);
                teamsResultList.add(mapToResponse(savedTeam));

            }

        }
        roundRepository.save(round);

        return CategoryRoundRankingResponse.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .advancementRule(round.getAdvancementRule())
                .topN(round.getTopN())
                .roundStatus(round.getStatus())
                .approvalSummary(CategoryRoundRankingResponse.ApprovalSummary.builder()
                        .totalTeamsProcessed(totalTeams)
                        .totalPassed(totalPassed)
                        .totalFailed(totalFailed).build())
                .teamsResult(teamsResultList).build();
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

        if (round.getStatus() != RoundStatus.EVALUATING &&
                round.getStatus() != RoundStatus.PUBLIC_DRAFT) {
            throw new BadRequestException("Vòng đấu đã đóng hoặc kết thúc, không thể thực hiện thao tác từ chối.");
        }

        // TRƯỜNG HỢP 1: Từ chối ngay lần đầu khi đang ở EVALUATING (Chưa tung bản nháp)
        if (round.getStatus() == RoundStatus.EVALUATING) {
            for (CategoryRound cr : categoryRound) {
                for (TeamParticipant team : cr.getTeamParticipants()) {

                    if (team.getEvaluations() != null) {
                        for (Evaluation evaluation : team.getEvaluations()) {
                            // CHUYỂN VỀ TRẠNG THÁI NOT GRADE TIẾN HÀNH CHẤM ĐIỂM LẠI
                            evaluation.setStatus(EvaluationStatus.NOT_GRADED);
                        }
                    }
                }
            }
        } else if (round.getStatus() == RoundStatus.PUBLIC_DRAFT) {

            // Trường hợp 2: Đang ở bản nháp (có khiếu nại) mà bấm từ chối. Chuyển về EVALUATING để chấm lại
            round.setStatus(RoundStatus.EVALUATING);

            for (CategoryRound cr : categoryRound) {
                for (TeamParticipant team : cr.getTeamParticipants()) {
                    if (team.getStatus() == ParticipantStatus.PASSED || team.getStatus() == ParticipantStatus.FAILED) {
                        team.setStatus(ParticipantStatus.ACTIVE);
                        team.setRank(null);
                        if (team.getEvaluations() != null) {
                            for (Evaluation evaluation : team.getEvaluations()) {
                                // CHUYỂN VỀ TRẠNG THÁI NOT GRADE TIẾN HÀNH CHẤM ĐIỂM LẠI
                                evaluation.setStatus(EvaluationStatus.NOT_GRADED);
                            }
                        }
                        participantRepository.save(team);
                    }
                }
            }
        }
        roundRepository.save(round);

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
        if (round.getStatus() != RoundStatus.EVALUATING) {
            throw new BadRequestException("Vòng thi không ở trạng thái đánh giá ");
        }
        List<CategoryRound> categoryRounds = round.getCategoryRounds();
        if (categoryRounds == null || categoryRounds.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này.");
        }
        boolean hasAprroved = round.getCategoryRounds().stream()
                .flatMap(categoryRound -> categoryRound.getTeamParticipants().stream())
                .anyMatch(teamParticipant -> teamParticipant.getStatus() == ParticipantStatus.PASSED
                        || teamParticipant.getStatus() == ParticipantStatus.FAILED);
        if (!hasAprroved) {
            throw new BadRequestException("Bạn cần phải duyệt bảng xếp hạng trước khi công bố kết quả tạm thời.");

        }
        // Công bố ranking nháp sau khi phê duyệt
        round.setStatus(RoundStatus.PUBLIC_DRAFT);
        roundRepository.save(round);
        log.info("Đã công bố bản nháp bảng xếp hạng vòng {}. Bắt đầu nhận phúc khảo.", roundId);


    }

    @Override
    public void publishFinalRanking(Integer roundId, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức vì vậy bạn không có quyền truy cập vào dữ liệu này."));

        // Check Round
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));
        if (round.getStatus() != RoundStatus.PUBLIC_DRAFT) {
            throw new BadRequestException("Vòng thi phải ở trạng thái PUBLIC_DRAFT mới có thể công bố chính thức. ");
        }

        // Công bố ranking nháp sau khi phê duyệt
        round.setStatus(RoundStatus.COMPLETED);
        roundRepository.save(round);
        log.info("Đã công bố bản xếp hạng chính thức vòng {}. Đóng vòng đấu thành công!", roundId);

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
        if (round.getStatus() != RoundStatus.PUBLIC_DRAFT) {
            throw new BadRequestException("Chưa tới thời điểm mở cổng đăng ký khiếu nại kết quả cuộc thi" +
                    "Hệ thống chỉ cho phép mở cổng khiếu nại sau khi công bố kết quả tạm thời.");
        }

        if (round.getAppealEndTime() != null) {
            throw new BadRequestException("Cổng khiếu nại đã được cấu hình lịch trước đó rồi.");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException(
                    "Thời gian kết thúc phải sau thời gian bắt đầu.");
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

    }

}
