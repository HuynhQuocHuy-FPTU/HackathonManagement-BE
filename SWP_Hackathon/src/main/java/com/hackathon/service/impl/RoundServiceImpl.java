package com.hackathon.service.impl;

import com.hackathon.dto.category.CategoryExpertAssignResponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.dto.round.UpdateRoundRequest;
import com.hackathon.dto.round.UpdateTimeRoundRequest;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.EvaluationCriteriaService;
import com.hackathon.service.ExpertAssignService;
import com.hackathon.service.RoundService;
import com.hackathon.validator.RoundValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
// Quản lý cấu hình, thời gian và danh sách các vòng thi thuộc một sự kiện.
public class RoundServiceImpl implements RoundService {

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private HackathonEventRepository eventRepository;

    @Autowired
    private EvaluationCriteriaService evaluationCriteriaService;

    @Autowired
    private RoundValidator roundValidator;

    @Autowired
    private ExpertAssignService expertAssignService;

    @Autowired
    private CriteriaSetRepository criteriaSetRepository;
    @Autowired
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Override
    // Kiểm tra lịch thi rồi tạo một vòng mới và gắn vòng đó vào đúng sự kiện.
    public Round createRound(CreateRoundRequest request, int eventId) throws BadRequestException {

        //1. Get hackathon event & criteria set
        HackathonEvent event = eventRepository.findById(eventId).orElseThrow(() -> new
                RuntimeException("Not found event with ID: " + eventId));
        if(request.getCriteriaSetId() != null){
            CriteriaSet criteriaSet = criteriaSetRepository.findById(request.getCriteriaSetId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy criteria set ") );
        }


        //lấy round đã tạo rồi dưới database lên
        List<Round> currentRounds = roundRepository.findAllByHackathonEvent_EventId(eventId);

        // 2. Validation
        roundValidator.validatorCreate(request, event);
        roundValidator.validateTimelineByOrderIndex(request, currentRounds);

        //3. Create round
        Round round = new Round();
        round.setRoundName(request.getRoundName());
        round.setStartTime(request.getStartDate());
        round.setEndTime(request.getEndDate());
        round.setAdvancementRule(request.getAdvancementRule());
        round.setTopN(request.getTopN());
        round.setSubmissionDeadline(request.getSubmissionDeadline());
        round.setEvaluationDeadline(request.getEvaluationDeadline());
        round.setResolveAppealDeadline(request.getResolveAppealDeadline());
        round.setSubmissionType(request.getSubmissionType());
        round.setAllowedFileType(request.getAllowedFileTypes());
        round.setMaxFileCount(request.getMaxFileCount());
        round.setStatus(RoundStatus.UPCOMING);
        round.setOrderIndex(request.getOrderIndex());
        if(request.getCriteriaSetId() != null){
            CriteriaSet criteriaSet = criteriaSetRepository.findById(request.getCriteriaSetId()).orElseThrow(() -> new BadRequestException("Không tìm thấy criteria set ") );
            round.setCriteriaSet(criteriaSet);
        }
        round.setHackathonEvent(event);

        //4. Save DB
        Round savedRound = roundRepository.save(round);

        if(request.getCriteriaSetId() != null){
            //5. Custom criteria
            BigDecimal totalWeight = BigDecimal.ZERO;
            BigDecimal hundred = new BigDecimal("100");
            if (request.getCustomCriteriaDetatils() != null && !request.getCustomCriteriaDetatils().isEmpty()) {
                for (EvaluationCriteriaRequestDTO customCriteria : request.getCustomCriteriaDetatils()) {
                    evaluationCriteriaService.createEvaluationCritera(customCriteria, request.getCriteriaSetId(), savedRound);
                    totalWeight = totalWeight.add(customCriteria.getCustomWeight());
                }
                evaluationCriteriaRepository.flush();
                if(totalWeight.compareTo(hundred) != 0){
                    throw new BadRequestException("Tổng trọng số phải bằng 100");
                }
            }
            else {
                throw new BadRequestException("Danh sách tiêu chí không được để trống.");
            }
        }
            return roundRepository.findById(savedRound.getRoundId()).orElse(savedRound);
    }



    @Override
    public RoundResponse mapToResponse(Round round) {

        if(round == null){
            return null;
        }

        List<EvaluationCriteriaResponseDTO> criteriaResponses = evaluationCriteriaService.getEvaluationCriteriaResponse(round);

        List<CategoryExpertAssignResponseDTO> expertResponse = expertAssignService.getExpertAssignmentsByRound(round);

        return new RoundResponse(round, criteriaResponses, expertResponse);
    }

    @Override
    @Transactional
    // Cập nhật một vòng thi và kiểm tra lịch mới không xung đột với các vòng còn lại.
    public Round updateSingleRound(UpdateRoundRequest roundRequest, List<Round> currentRounds, Integer eventId) throws BadRequestException {
        Round saveRound;

        if (roundRequest.getRoundId() != null) {
            // --- TRƯỜNG HỢP SỬA ROUND CŨ ---

            // 1. Tìm round cần sửa trong danh sách hiện tại (đã có sẵn trên RAM, không query lại DB)
            saveRound = currentRounds.stream()
                    .filter(r -> r.getRoundId().equals(roundRequest.getRoundId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy round: " + roundRequest.getRoundId()));

            // 2. Kiểm tra logic ngày giờ, hạn nộp bài cơ bản của chính round này
            roundValidator.validatorUpdate(roundRequest);

            // 3. Kiểm tra dòng thời gian so với các round khác trong cùng Event
            roundValidator.validateTimelineByOrderIndexUpdate(roundRequest, currentRounds);

            // 4. Cập nhật thông tin round
            saveRound.setRoundName(roundRequest.getRoundName());
            saveRound.setStartTime(roundRequest.getStartDate());
            saveRound.setEndTime(roundRequest.getEndDate());
            saveRound.setAdvancementRule(roundRequest.getAdvancementRule());
            saveRound.setTopN(roundRequest.getTopN());
            saveRound.setOrderIndex(roundRequest.getOrderIndex());
            saveRound.setSubmissionDeadline(roundRequest.getSubmissionDeadline());
            saveRound.setEvaluationDeadline(roundRequest.getEvaluationDeadline());
            saveRound.setResolveAppealDeadline(roundRequest.getResolveAppealDeadline());
            saveRound.setSubmissionType(roundRequest.getSubmissionType());
            saveRound.setAllowedFileType(roundRequest.getAllowedFileTypes());
            saveRound.setMaxFileCount(roundRequest.getMaxFileCount());
            // 5. Lấy CriteriaSet tương ứng
            if(roundRequest.getCriteriaSetId() != null){
                CriteriaSet criteriaSet = criteriaSetRepository.findById(roundRequest.getCriteriaSetId())
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy criteria set"));
                saveRound.setCriteriaSet(criteriaSet);

            }

        } else {
            // --- TRƯỜNG HỢP TẠO MỚI -----
            saveRound = this.createRound(roundRequest, eventId);
        }

        // 7. Chèn lại Custom Criteria mới từ request (áp dụng cho cả sửa lẫn tạo mới)
        if (roundRequest.getCriteriaSetId() != null) {
            // Validate dữ liệu tiêu chí trước khi xóa dữ liệu cũ
            if (roundRequest.getCustomCriteriaDetatils() == null || roundRequest.getCustomCriteriaDetatils().isEmpty()) {
                throw new BadRequestException("Danh sách tiêu chí không được để trống.");
            }

            BigDecimal totalWeight = roundRequest.getCustomCriteriaDetatils().stream()
                    .map(c -> c.getCustomWeight() != null ? c.getCustomWeight() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
                throw new BadRequestException("Tổng trọng số phải bằng 100");
            }

            // Thực hiện xóa và cập nhật
            evaluationCriteriaRepository.deleteByRound_RoundId(saveRound.getRoundId());

            for (EvaluationCriteriaRequestDTO customCriteria : roundRequest.getCustomCriteriaDetatils()) {
                evaluationCriteriaService.createEvaluationCritera(customCriteria, roundRequest.getCriteriaSetId(), saveRound);
            }
        }
        // 8. Lưu và trả về round đã đồng bộ
        return roundRepository.saveAndFlush(saveRound);
    }
    @Override
    @Transactional
    // Xóa các vòng hiện tại không còn xuất hiện trong danh sách cập nhật của sự kiện.
    public List<Round> deleteRoundsExcluding(List<UpdateRoundRequest> roundRequests, List<Round> currentRounds) {

        // 1. Lấy danh sách ID của các Round mà Frontend gửi lên (chỉ lấy những cái có ID, tức là Round cũ cần giữ lại)
        List<Integer> incomingRoundIds = roundRequests.stream()
                .map(UpdateRoundRequest::getRoundId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 2. Tìm các Round hiện tại trong DB mà không nằm trong danh sách Frontend gửi lên -> đây là các Round cần xóa
        List<Round> roundsToDelete = currentRounds.stream()
                .filter(current -> !incomingRoundIds.contains(current.getRoundId()))
                .collect(Collectors.toList());

        // 3. Xóa các Round không còn được sử dụng và flush ngay để tránh conflict khi các bước sau query lại
        if (!roundsToDelete.isEmpty()) {
            roundRepository.deleteAll(roundsToDelete);
            roundRepository.flush();
        }

        // 4. Trả về danh sách Round còn lại sau khi xóa
        return currentRounds.stream()
                .filter(current -> incomingRoundIds.contains(current.getRoundId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    // Xóa toàn bộ vòng thi thuộc sự kiện được chỉ định.
    public void deleteByEventId(Integer eventId) {
        List<Round> rounds = roundRepository.findAllByHackathonEvent_EventId(eventId);
        if (!rounds.isEmpty()) {
            // Ép load các quan hệ LAZY lên RAM trước khi xóa
            rounds.forEach(round -> {
                round.getEvaluationCriterias().size();
                round.getCategoryRounds().size();
            });
            roundRepository.deleteAll(rounds);
            roundRepository.flush();
        }
    }

    @Override
    @Transactional
    // Lấy tất cả vòng thi của một sự kiện theo mã sự kiện.
    public List<Round> findAllByEventId(Integer eventId) {
        List<Round> rounds = new ArrayList<>();
        rounds = roundRepository.findAllByHackathonEvent_EventId(eventId);
        return rounds;
    }

    @Override
    // Lấy các vòng thi có trạng thái khác trạng thái cần loại trừ.
    public List<Round> getRoundByStatusNot(RoundStatus status) {

        List<Round> rounds = roundRepository.findByStatusNot(status);
        return rounds;
    }

    @Override
    // Lưu thông tin vòng thi mới hoặc các thay đổi của vòng thi hiện có.
    public Round saveRound(Round round) {
        return roundRepository.save(round);
    }

    @Override
    // Tìm vòng thi theo mã và trả về kết quả có thể rỗng nếu không tồn tại.
    public Optional<Round> findById(Integer roundId) {
        return roundRepository.findById(roundId);
    }

    @Override
    @Transactional
    // Cập nhật lịch của vòng thi sau khi kiểm tra quyền, thứ tự thời gian và giới hạn sự kiện.
    public void updateTimeRound(UpdateTimeRoundRequest updateTimeRoundRequest, CustomUserDetails userDetails, Integer roundId) {

        EventCoordinator eventCoordinator = userDetails.getAccount().getEventCoordinator();

        if(eventCoordinator == null){
            throw new BadRequestException("Bạn không có quyền truy cập. Bạn phải là event coordinator");
        }

        Round round = roundRepository.findById(roundId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vòng thi"));

        roundValidator.validateTimeRound(round, updateTimeRoundRequest);

        LocalDateTime oldEvaluationDeadline = round.getEvaluationDeadline();
        LocalDateTime newEvaluationDeadline =
                updateTimeRoundRequest.getEvaluationDeadline();
        boolean isEvaluationDeadlineChanged =
                oldEvaluationDeadline != null
                        && newEvaluationDeadline != null
                        && !newEvaluationDeadline.equals(oldEvaluationDeadline);

        round.setStartTime(updateTimeRoundRequest.getStartDate());
        round.setEndTime(updateTimeRoundRequest.getEndDate());
        round.setSubmissionDeadline(updateTimeRoundRequest.getSubmissionDeadline());
        round.setEvaluationDeadline(newEvaluationDeadline);
        round.setResolveAppealDeadline(updateTimeRoundRequest.getResolveAppealDeadline());

        if (isEvaluationDeadlineChanged) {
            round.setScoringProcessedAt(null);
            round.setScoringFailureNotifiedAt(null);
        }

        roundRepository.save(round);
    }


}
