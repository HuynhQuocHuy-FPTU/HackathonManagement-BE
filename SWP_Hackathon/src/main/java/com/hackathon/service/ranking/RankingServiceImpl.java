package com.hackathon.service.ranking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.dto.participant.ParticipantResponseDTO;
import com.hackathon.dto.ranking.CategoryRankingResponse;
import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.RankingResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.AuditService;

import com.hackathon.service.ExcelExportService;
import com.hackathon.service.NotificationService;
import com.hackathon.service.RoundAdvancementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {
    private final AuditService auditService;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final RoundRepository roundRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationService notificationService;
    private final ExcelExportService excelExportService;
    private final RoundAdvancementService roundAdvancementService;
    private final AccountRepository accountRepository;


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

    // Khi chấm điểm xong thì sẽ public Draft
    @Override
    @Transactional
    public void publishDraftRankingAndOpenAppeals(Integer roundId, CustomUserDetails userDetails, Integer hoursAmount) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức vì vậy bạn không có quyền truy cập vào dữ liệu này."));
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));

        if (round.getStatus() != RoundStatus.PENDING) {
            throw new BadRequestException("Vòng đấu phải ở trạng thái PENDING mới có thể công bố kết quả.");
        }

        if (hoursAmount == null || hoursAmount <= 0) {
            throw new BadRequestException("Vui lòng nhập số giờ mở cổng khiếu nại hợp lệ (lớn hơn 0).");
        }

        List<CategoryRound> categoryRounds = round.getCategoryRounds();
        if (categoryRounds == null || categoryRounds.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này.");
        }
        // check total score vs ranking mới dc publish

        // Công bố ranking nháp  và lưu log
        String uploadUrl = excelExportService.exportRankingToExcel(roundId, "DRAFT");
        updateAndSaveExcelJson(round, uploadUrl, "DRAFT");
        round.setStatus(RoundStatus.APPEALING);
        round.setAppealStartTime(LocalDateTime.now());
        round.setAppealEndTime(LocalDateTime.now().plusHours(hoursAmount));
        roundRepository.save(round);
        log.info("Đã công bố bản nháp bảng xếp hạng vòng {}. Bắt đầu nhận phúc khảo.", roundId);

        List<CategoryRankingResponse> auditRankingData = auditRankingData(categoryRounds);
        try {
            auditService.saveLog(
                    account,
                    AuditAction.SAVE_DRAFT,
                    AuditEntityType.ROUND,
                    roundId,
                    "Công bố kết quả tạm thời thành công.",
                    objectMapper.writeValueAsString(auditRankingData)
            );
        } catch (JsonProcessingException e) {

            log.error("Lỗi khi tuần tự hóa dữ liệu xếp hạng vòng {} sang JSON", roundId, e);
            throw new BadRequestException(" Không thể lưu lịch sử bảng xếp hạng do lỗi hệ thống.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishFinalRanking(Integer roundId) {
        // Check Round
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));

        // Công bố ranking chính thức sau khi phê duyệt và kết thúc thời gian phúc khảo
        if (round.getResolveAppealDeadline() != null
                && LocalDateTime.now().isBefore(round.getResolveAppealDeadline())) {
            throw new BadRequestException(
                    "Chưa tới thời gian công bố kết quả cuối"
            );
        }
        log.info("Round status = {}", round.getStatus());


        List<CategoryRound> categoryRounds = round.getCategoryRounds();
        if (categoryRounds == null || categoryRounds.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này.");
        }

        for (CategoryRound cr : categoryRounds) {
            log.info("Step 1");
            log.info("Before advance");

            roundAdvancementService.calculateScoresAndRanking(cr.getCategoryRoundId());
        }
        log.info("Step 2");
        log.info("After advance");

        roundAdvancementService.advanceAllCategoriesInRound(roundId);

        // 3. REFRESH DATA TRONG HIBERNATE SESSION ĐỂ TRÁNH LẤY ĐIỂM/RANK CŨ TRONG CACHE
        roundRepository.flush();
        round = roundRepository.findById(roundId).orElseThrow();
        categoryRounds = round.getCategoryRounds();

        log.info("Bắt đầu export FINAL Excel round {}", roundId);

        String uploadUrl = excelExportService.exportRankingToExcel(roundId, "FINAL");
        updateAndSaveExcelJson(round, uploadUrl, "FINAL");
        log.info("Export thành công: {}", uploadUrl);


        round.setStatus(RoundStatus.FINAL_RESULT);
        roundRepository.save(round);
        log.info("Đã công bố bản xếp hạng chính thức vòng {}. ", roundId);
        notificationService.notifyRoundRankingPublished(null, roundId, true);

        List<CategoryRankingResponse> auditRankingData = auditRankingData(categoryRounds);

        Account systemAccount = accountRepository
                .findByEmail("system@hackathon.com")
                .orElseThrow();
        try {

            auditService.saveLog(
                    systemAccount,
                    AuditAction.PUBLISH_FINAL,
                    AuditEntityType.ROUND,
                    roundId,
                    "Công bố bản xếp hạng chính thức của vòng " + round.getRoundName() + " thành công",
                    objectMapper.writeValueAsString(auditRankingData)
            );
        } catch (JsonProcessingException e) {
            log.error("Lỗi khi tuần tự hóa dữ liệu xếp hạng vòng {} sang JSON", roundId, e);
            throw new BadRequestException("Không thể lưu lịch sử bảng xếp hạng do lỗi hệ thống.");
        }
    }

    private List<CategoryRankingResponse> auditRankingData(List<CategoryRound> categoryRounds) {
        return categoryRounds.stream().map(cr -> {
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

    }

    @Override
    public CategoryRoundRankingResponse getTopNRanking(Integer roundId) {
        Round round = roundRepository.findById(roundId).orElseThrow(
                () -> new BadRequestException("Không tìm thấy vòng thi"));
        if (round.getStatus() != RoundStatus.COMPLETED
        && round.getStatus()!=  RoundStatus.FINAL_RESULT) {
            throw new BadRequestException("Bạn không được phép xem bảng xếp hạng khi vòng thi chưa hoàn thành.");
        }
        List<CategoryRound> categoryRound = round.getCategoryRounds();
        int topN = round.getTopN();

        List<CategoryRankingResponse> categoriesRanking = new ArrayList<>();
        for (CategoryRound cr : categoryRound) {
            // Thông qua category Round lấy top N ranking
            List<TeamParticipant> tp = cr.getTeamParticipants().stream()
                    .filter(teamParticipant -> teamParticipant.getRank() <= topN)
                    .sorted(Comparator.comparing(TeamParticipant::getRank))
                    .toList();

            List<RankingResponseDTO> rankingResponse = new ArrayList<>();

            for (TeamParticipant participant : tp) {
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
                .advancementRule(round.getAdvancementRule())
                .topN(round.getTopN())
                .orderIndex(round.getOrderIndex())
                .roundStatus(round.getStatus())
                .categoriesRanking(categoriesRanking)
                .build();
    }

    @Override
    public CategoryRoundRankingResponse getRankingByAll(Integer roundId, CustomUserDetails userDetails) {

        Account account = userDetails.getAccount();
        if (account == null) {
            throw new BadRequestException("Bạn chưa đăng nhập tài khoản.");
        }
        boolean isEvenCoordinator = account.getRole().equals(AccountRole.EVENTCOORDINATOR);

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thây vòng thi."));

        //  Đang chấm hoặc chờ duyệt , event moiws dc voaf
        if (round.getStatus() == RoundStatus.EVALUATING
                || round.getStatus() == RoundStatus.RE_EVALUATING) {
            if (!isEvenCoordinator) {
                throw new BadRequestException("Bảng xếp hạng đang được chấm và kiểm duyệt. Bạn không được phép truy cập");
            }
        }

        List<CategoryRankingResponse> categoriesRanking = new ArrayList<>();

        // dang phuc khao thi sinh chi dc xem qua file excel
        if (round.getStatus() == RoundStatus.APPEALING) {
            return CategoryRoundRankingResponse.builder()
                    .roundId(round.getRoundId())
                    .roundName(round.getRoundName())
                    .orderIndex(round.getOrderIndex())
                    .advancementRule(round.getAdvancementRule())
                    .topN(round.getTopN())
                    .roundStatus(round.getStatus())
                    .draftExcelUrl(round.getExcelsUrl())
                    .categoriesRanking(categoriesRanking).build();
        }
        if (round.getCategoryRounds() != null) {
            for (CategoryRound cr : round.getCategoryRounds()) {

                List<RankingResponseDTO> rankingResponse = cr.getTeamParticipants().stream()
                        .sorted(Comparator.comparing(TeamParticipant::getRank, Comparator.nullsLast(Integer::compareTo)))
                        .map(participant -> {
                            String teamName = "N/A";
                            if (participant.getRegistration() != null && participant.getRegistration().getTeam() != null) {
                                teamName = participant.getRegistration().getTeam().getTeamName();
                            }

                            return RankingResponseDTO.builder()
                                    .participantId(participant.getId())
                                    .totalScore(participant.getTotalScore())
                                    .rank(participant.getRank())
                                    .teamName(teamName)
                                    .status(participant.getStatus())
                                    .build();
                        })
                        .toList();

                CategoryRankingResponse response = CategoryRankingResponse.builder()
                        .categoryRoundId(cr.getCategoryRoundId())
                        .categoryId(cr.getCategory() != null ? cr.getCategory().getCategoryId() : null)
                        .categoryName(cr.getCategory() != null ? cr.getCategory().getCategoryName() : "N/A")
                        .teams(rankingResponse)
                        .build();

                categoriesRanking.add(response);
            }
        }

        // 6. Trả về Response đầy đủ thông tin
        return CategoryRoundRankingResponse.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .orderIndex(round.getOrderIndex())
                .advancementRule(round.getAdvancementRule())
                .topN(round.getTopN())
                .roundStatus(round.getStatus())
                .draftExcelUrl(round.getExcelsUrl())
                .categoriesRanking(categoriesRanking)
                .build();
    }


    @Override
    public String getRankingPublicExcels(Integer roundId, String type) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi"));
        try {

            List<Map<String, Object>> files =
                    objectMapper.readValue(
                            round.getExcelsUrl(),
                            new TypeReference<List<Map<String, Object>>>() {
                            }
                    );

            return files.stream()
                    .filter(file -> type.equals(file.get("type")))
                    .map(file -> file.get("url").toString())
                    .findFirst()
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Chưa có file " + type + " được công bố"
                            )
                    );

        } catch (JsonProcessingException e) {
            throw new BadRequestException(
                    "Lỗi đọc dữ liệu file Excel"
            );
        }


    }

    private void updateAndSaveExcelJson(Round round, String url, String fileType) {
        List<Map<String, Object>> currentFiles = new ArrayList<>();
        String oldJson = round.getExcelsUrl();

        if (oldJson != null && !oldJson.trim().isEmpty()) {
            try {
                currentFiles = objectMapper.readValue(oldJson, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
                });
            } catch (Exception e) {
                currentFiles = new ArrayList<>();
            }
        }
        // Thêm bản ghi file mới
        Map<String, Object> newExcelFile = new HashMap<>();
        newExcelFile.put("version", currentFiles.size() + 1);
        newExcelFile.put("type", fileType); // Lưu lại trạng thái của round trước khi đổi
        newExcelFile.put("url", url);
        newExcelFile.put("createdAt", java.time.LocalDateTime.now().toString());
        currentFiles.add(newExcelFile);

        try {
            round.setExcelsUrl(objectMapper.writeValueAsString(currentFiles));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Lỗi hệ thống khi tuần tự hóa dữ liệu file Excel.");
        }
    }


}
