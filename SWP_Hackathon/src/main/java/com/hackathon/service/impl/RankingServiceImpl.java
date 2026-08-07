package com.hackathon.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.dto.ranking.CategoryRankingResponse;
import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.RankingResponseDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.NotificationService;
import com.hackathon.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
// Quản lý việc xem, công bố và lưu các phiên bản bảng xếp hạng của vòng thi.
public class RankingServiceImpl implements RankingService {
    private final AuditService auditService;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final RoundRepository roundRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationService notificationService;
    private final ExcelExportService excelExportService;
    //===============================================//
    //RANKING
    //===============================================



    @Override
    public CategoryRoundRankingResponse getRankingByEventCoordinator(
            Integer roundId, CustomUserDetails userDetails) {
        // Lấy tài khoản hiện tại để xác định người yêu cầu có thuộc ban tổ chức hay không.
        Account account = userDetails.getAccount();
        // Chỉ tài khoản có hồ sơ ban tổ chức mới được xem bảng xếp hạng quản trị.
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Event Coordinator."));

        // Tìm vòng thi cần xem và dừng xử lý khi mã vòng không tồn tại.
        Round round = roundRepository.findById(roundId).orElseThrow(
                () -> new BadRequestException("Không tìm thấy vòng thi này."));

        List<CategoryRankingResponse> categoriesRanking = new ArrayList<>();
        for (CategoryRound cr : round.getCategoryRounds()) {
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


    // Khi chấm điểm xong thì sẽ public Draft
    @Override
    @Transactional
    public void publishDraftRankingAndOpenAppeals(Integer roundId, CustomUserDetails userDetails, Integer minutesAmount) {
        // Lấy tài khoản làm người thực hiện trong lịch sử công bố kết quả.
        Account account = userDetails.getAccount();
        // Xác nhận tài khoản hiện tại thật sự là ban tổ chức.
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức vì vậy bạn không có quyền truy cập vào dữ liệu này."));
        // Tìm vòng thi cần công bố bảng xếp hạng tạm thời.
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));

        // Chỉ vòng đang chờ công bố kết quả mới được chuyển sang giai đoạn khiếu nại.
        if (round.getStatus() != RoundStatus.PENDING) {
            throw new BadRequestException("Vòng đấu phải ở trạng thái PENDING mới có thể công bố kết quả.");
        }

        // Khoảng thời gian mở cổng khiếu nại phải là một số phút dương.
        if (minutesAmount == null || minutesAmount <= 0) {
            throw new BadRequestException("Vui lòng nhập số phút mở cổng khiếu nại hợp lệ (lớn hơn 0).");
        }

        // Hạn giải quyết khiếu nại phải còn nằm trong tương lai tại thời điểm công bố.
        if(!round.getResolveAppealDeadline().isAfter(LocalDateTime.now())){
            throw new BadRequestException(
                    "Thời gian giải quyết khiếu nại phải sau thời điểm hiện tại."
            );
        }

        // Cổng nhận đơn phải đóng trước hoặc đúng hạn giải quyết cuối cùng của ban tổ chức.
        if(LocalDateTime.now().plusMinutes(minutesAmount).isAfter(round.getResolveAppealDeadline())){
            throw new BadRequestException("Thời gian kết thúc nhận đơn khiếu nại không được phép sau thời gian giải quyết khiếu nại");
        }

        // Vòng phải có ít nhất một danh mục thì mới có dữ liệu xếp hạng để công bố.
        List<CategoryRound> categoryRounds = round.getCategoryRounds();
        if (categoryRounds == null || categoryRounds.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này.");
        }

        // Xuất bảng xếp hạng tạm thời thành tệp Excel và nhận đường dẫn đã tải lên.
        String uploadUrl = excelExportService.exportRankingToExcel(roundId, "DRAFT");
        // Thêm đường dẫn tệp tạm thời vào lịch sử các phiên bản Excel của vòng.
        updateAndSaveExcelJson(round, uploadUrl, "DRAFT");
        // Chuyển vòng sang trạng thái đang tiếp nhận đơn khiếu nại.
        round.setStatus(RoundStatus.APPEALING);
        // Ghi nhận thời điểm bắt đầu nhận đơn là thời điểm công bố hiện tại.
        round.setAppealStartTime(LocalDateTime.now());
        // Tính thời điểm đóng cổng dựa trên khoảng thời gian được cung cấp.
        round.setAppealEndTime(LocalDateTime.now().plusMinutes(minutesAmount));
        // Lưu trạng thái và các mốc thời gian mới của vòng.
        roundRepository.save(round);
        log.info("Đã công bố bản nháp bảng xếp hạng vòng {}. Bắt đầu nhận phúc khảo.", roundId);
        // Gửi thông báo công bố kết quả tạm thời đến các đội trong vòng.
        notificationService.notifyRoundRankingPublished(eventCoordinator.getAccount(), roundId, false, minutesAmount);

        // Chuẩn bị ảnh chụp dữ liệu xếp hạng dùng để lưu lịch sử kiểm toán.
        List<CategoryRankingResponse> auditRankingData = auditRankingData(categoryRounds);
        // Chuyển dữ liệu lịch sử thành chuỗi và ghi lại thao tác công bố.
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

            // Không để giao dịch hoàn tất nếu dữ liệu lịch sử không thể được lưu đúng định dạng.
            log.error("Lỗi khi tuần tự hóa dữ liệu xếp hạng vòng {} sang JSON", roundId, e);
            throw new BadRequestException(" Không thể lưu lịch sử bảng xếp hạng do lỗi hệ thống.");
        }
    }

    @Override
    @Transactional
    public void publishFinalRanking(Integer roundId , CustomUserDetails userDetails) {
        // Xác nhận người công bố kết quả cuối cùng là thành viên ban tổ chức.
        EventCoordinator coordinator = eventCoordinatorRepository.findByAccount_AccountId(userDetails.getAccount().getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức."));
        // Tìm vòng thi cần công bố kết quả chính thức.
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));

        // Chỉ công bố khi ban tổ chức đã hết thời hạn giải quyết khiếu nại.
        if (round.getResolveAppealDeadline() != null
                && LocalDateTime.now().isBefore(round.getResolveAppealDeadline())) {
            throw new BadRequestException(
                    "Chưa tới thời gian công bố kết quả cuối"
            );
        }

        // Vòng không có danh mục sẽ không có dữ liệu xếp hạng hợp lệ để công bố.
        List<CategoryRound> categoryRounds = round.getCategoryRounds();
        if (categoryRounds == null || categoryRounds.isEmpty()) {
            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này.");
        }

        // Đẩy thay đổi đang chờ xuống cơ sở dữ liệu để tránh xuất điểm hoặc thứ hạng cũ.
        roundRepository.flush();
        // Tải lại vòng cùng dữ liệu xếp hạng mới nhất sau khi đồng bộ.
        round = roundRepository.findById(roundId).orElseThrow();
        // Lấy lại danh mục từ đối tượng vòng vừa được tải mới.
        categoryRounds = round.getCategoryRounds();

        log.info("Bắt đầu export FINAL Excel round {}", roundId);

        // Xuất phiên bản bảng xếp hạng chính thức thành tệp Excel.
        String uploadUrl = excelExportService.exportRankingToExcel(roundId, "FINAL");
        // Lưu đường dẫn phiên bản chính thức vào lịch sử tệp của vòng.
        updateAndSaveExcelJson(round, uploadUrl, "FINAL");
        log.info("Export thành công: {}", uploadUrl);


        // Đánh dấu vòng đã có kết quả chính thức.
        round.setStatus(RoundStatus.FINAL_RESULT);
        // Lưu trạng thái cuối cùng và lịch sử tệp Excel.
        roundRepository.save(round);
        log.info("Đã công bố bản xếp hạng chính thức vòng {}. ", roundId);
        // Gửi thông báo kết quả chính thức đến các đội liên quan.
        notificationService.notifyRoundRankingPublished(null, roundId, true, null);

        // Chuẩn bị dữ liệu tại thời điểm công bố để lưu vết kiểm toán.
        List<CategoryRankingResponse> auditRankingData = auditRankingData(categoryRounds);


        try {

            auditService.saveLog(
                    coordinator.getAccount(),
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
        // Tìm vòng thi cần xem danh sách các đội có thứ hạng cao nhất.
        Round round = roundRepository.findById(roundId).orElseThrow(
                () -> new BadRequestException("Không tìm thấy vòng thi"));
        // Chỉ hiển thị kết quả khi vòng đã hoàn thành hoặc đã công bố kết quả chính thức.
        if (round.getStatus() != RoundStatus.COMPLETED
        && round.getStatus()!=  RoundStatus.FINAL_RESULT) {
            throw new BadRequestException("Bạn không được phép xem bảng xếp hạng khi vòng thi chưa hoàn thành.");
        }
        // Lấy danh sách danh mục và số lượng đội được chọn từ cấu hình vòng.
        List<CategoryRound> categoryRound = round.getCategoryRounds();
        int topN = round.getTopN();

        List<CategoryRankingResponse> categoriesRanking = new ArrayList<>();
        for (CategoryRound cr : categoryRound) {
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

        // Lấy tài khoản để kiểm tra trạng thái đăng nhập và vai trò xem kết quả.
        Account account = userDetails.getAccount();
        // Không cho phép truy cập bảng xếp hạng khi phiên không có tài khoản.
        if (account == null) {
            throw new BadRequestException("Bạn chưa đăng nhập tài khoản.");
        }
        // Ban tổ chức được xem dữ liệu trong một số trạng thái mà người dùng thường bị hạn chế.
        boolean isEvenCoordinator = account.getRole().equals(AccountRole.EVENTCOORDINATOR);

        // Tìm vòng thi cần xem bảng xếp hạng.
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thây vòng thi."));

        // Kiểm tra có đội nào đang được chấm lại nên kết quả vẫn có khả năng thay đổi hay không.
        boolean hasReEvaluating =round.getCategoryRounds().stream()
                .flatMap(cr -> cr.getTeamParticipants().stream())
                .anyMatch(tp -> tp.getStatus() == ParticipantStatus.RE_EVALUATING);

        // Người không thuộc ban tổ chức không được xem khi vòng đang chấm hoặc có đội đang chấm lại.
        if (!isEvenCoordinator &&
                (round.getStatus() == RoundStatus.EVALUATING || hasReEvaluating)) {
            throw new BadRequestException(
                    "Bảng xếp hạng đang được chấm hoặc chấm lại. Bạn không được phép truy cập.");
        }

        List<CategoryRankingResponse> categoriesRanking = new ArrayList<>();

        // Trong thời gian khiếu nại, chỉ trả đường dẫn tệp kết quả tạm thời thay vì dữ liệu trực tiếp.
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
        // Tìm vòng chứa danh sách các phiên bản tệp bảng xếp hạng.
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi"));
        try {

            // Đọc chuỗi lưu lịch sử tệp thành danh sách để tìm đúng loại được yêu cầu.
            List<Map<String, Object>> files =
                    objectMapper.readValue(
                            round.getExcelsUrl(),
                            new TypeReference<List<Map<String, Object>>>() {
                            }
                    );

            // Tìm tệp có loại trùng với yêu cầu và trả về đường dẫn công khai.
            return files.stream()
                    .filter(file -> type.equals(file.get("type")))
                    .map(file -> file.get("url").toString())
                    .findFirst()
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Chưa có file " + type + " được công bố"
                            )
                    );

            // Chuỗi lịch sử tệp sai định dạng được xem là lỗi dữ liệu của hệ thống.
        } catch (JsonProcessingException e) {
            throw new BadRequestException(
                    "Lỗi đọc dữ liệu file Excel"
            );
        }


    }

    private void updateAndSaveExcelJson(Round round, String url, String fileType) {
        // Khởi tạo danh sách rỗng để hỗ trợ cả vòng chưa từng xuất tệp.
        List<Map<String, Object>> currentFiles = new ArrayList<>();
        // Lấy chuỗi lịch sử các tệp đã được lưu trong vòng.
        String oldJson = round.getExcelsUrl();

        // Chỉ đọc dữ liệu cũ khi chuỗi có nội dung hợp lệ để xử lý.
        if (oldJson != null && !oldJson.trim().isEmpty()) {
            try {
                // Khôi phục danh sách phiên bản tệp đã công bố trước đó.
                currentFiles = objectMapper.readValue(oldJson, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
                });
            } catch (Exception e) {
                // Nếu dữ liệu cũ bị lỗi, dùng danh sách mới để quá trình công bố vẫn có thể tiếp tục.
                currentFiles = new ArrayList<>();
            }
        }
        // Tạo thông tin cho phiên bản tệp vừa được xuất.
        Map<String, Object> newExcelFile = new HashMap<>();
        // Số phiên bản mới tăng dần theo số tệp hiện có.
        newExcelFile.put("version", currentFiles.size() + 1);
        // Lưu loại tệp để phân biệt kết quả tạm thời và kết quả chính thức.
        newExcelFile.put("type", fileType);
        // Lưu đường dẫn tải xuống của tệp.
        newExcelFile.put("url", url);
        // Ghi lại thời điểm tạo phiên bản để theo dõi lịch sử công bố.
        newExcelFile.put("createdAt", java.time.LocalDateTime.now().toString());
        // Thêm phiên bản mới vào cuối danh sách hiện tại.
        currentFiles.add(newExcelFile);

        try {
            // Lưu lại toàn bộ lịch sử tệp dưới dạng chuỗi trong vòng thi.
            round.setExcelsUrl(objectMapper.writeValueAsString(currentFiles));
        } catch (JsonProcessingException e) {
            // Dừng công bố nếu lịch sử tệp không thể chuyển thành định dạng lưu trữ.
            throw new BadRequestException("Lỗi hệ thống khi tuần tự hóa dữ liệu file Excel.");
        }
    }


}
