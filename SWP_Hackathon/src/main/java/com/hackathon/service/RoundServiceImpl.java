package com.hackathon.service;

import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.category.CategoryExpertAssignResponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.round.CreateRoundRequest;
import com.hackathon.dto.round.RoundResponse;
import com.hackathon.dto.round.UpdateRoundRequest;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.validator.RoundValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoundServiceImpl implements RoundService{

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private HackathonEventRepository eventRepository;

    @Autowired
    private CriteriaDetailRepository criteriaDetailRepository;

    @Autowired
    private EvaluationCriteriaService evaluationCriteriaService;

    @Autowired
    private RoundValidator roundValidator;

    @Autowired
    private ExpertAssignService expertAssignService;

    @Autowired
    private ExpertAssignRepository expertAssignRepository;

    @Autowired
    private CriteriaSetRepository criteriaSetRepository;
    @Autowired
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Override
    public Round createRound(CreateRoundRequest request, int eventId) throws BadRequestException {

        //1. Get hackathon event & criteria set
        HackathonEvent event = eventRepository.findById(eventId).orElseThrow(() -> new
                RuntimeException("Not found event with ID: " + eventId));
        CriteriaSet criteriaSet = criteriaSetRepository.findById(request.getCriteriaSetId()).orElseThrow(() -> new BadRequestException("Không tìm thấy criteria set ") );

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
        round.setSubmissionDeadline(request.getSubmissionDeadline());
        round.setStatus(RoundStatus.UPCOMING);
        round.setOrderIndex(request.getOrderIndex());
        round.setCriteriaSet(criteriaSet);
        round.setHackathonEvent(event);

        //4. Save DB
        Round savedRound = roundRepository.save(round);

        //5. Custom criteria
        if (request.getCustomCriteriaDetatils() != null && !request.getCustomCriteriaDetatils().isEmpty()) {
            for (EvaluationCriteriaRequestDTO customCriteria : request.getCustomCriteriaDetatils()) {
                    evaluationCriteriaService.createEvaluationCritera(customCriteria, request.getCriteriaSetId(), savedRound);
                }
            evaluationCriteriaRepository.flush();

            }
            return roundRepository.findById(savedRound.getRoundId()).orElse(savedRound);
    }



    @Override
    public RoundResponse mapToResponse(Round round) {

        if(round == null){
            return null;
        }

        //1. Map danh sách tiêu chí chấm điểm từ entity sang Response
        List<EvaluationCriteriaResponseDTO> criteriaResponses = evaluationCriteriaService.getEvaluationCirteriaResponse(round);

        //2. map danh sách expert sang response
        List<CategoryExpertAssignResponseDTO> expertResponse = expertAssignService.getExpertAssignmentsByRound(round);

        return new RoundResponse(round, criteriaResponses, expertResponse);
    }

    @Override
    @Transactional
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

            // 4. Lấy CriteriaSet tương ứng
            CriteriaSet criteriaSet = criteriaSetRepository.findById(roundRequest.getCriteriaSetId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy criteria set"));

            // 5. Cập nhật thông tin round
            saveRound.setRoundName(roundRequest.getRoundName());
            saveRound.setStartTime(roundRequest.getStartDate());
            saveRound.setEndTime(roundRequest.getEndDate());
            saveRound.setAdvancementRule(roundRequest.getAdvancementRule());
            saveRound.setOrderIndex(roundRequest.getOrderIndex());
            saveRound.setSubmissionDeadline(roundRequest.getSubmissionDeadline());
            saveRound.setCriteriaSet(criteriaSet);

            // 6. Xóa các tiêu chí cũ trước khi chèn lại tiêu chí mới từ request
            if (saveRound.getEvaluationCriterias() != null && !saveRound.getEvaluationCriterias().isEmpty()) {
                saveRound.getEvaluationCriterias().clear();
                roundRepository.saveAndFlush(saveRound);
            }

        } else {
            // --- TRƯỜNG HỢP TẠO MỚI ---
            // Gọi sang createRound, bên trong đã có sẵn validator nên an toàn
            saveRound = this.createRound(roundRequest, eventId);
        }

        // 7. Chèn lại Custom Criteria mới từ request (áp dụng cho cả sửa lẫn tạo mới)
        if (roundRequest.getCustomCriteriaDetatils() != null && !roundRequest.getCustomCriteriaDetatils().isEmpty()) {
            for (EvaluationCriteriaRequestDTO customCriteria : roundRequest.getCustomCriteriaDetatils()) {
                evaluationCriteriaService.createEvaluationCritera(customCriteria, roundRequest.getCriteriaSetId(), saveRound);
            }
            // Ép đồng bộ tiêu chí mới xuống DB ngay lập tức
            evaluationCriteriaRepository.flush();
        }

        // 8. Lưu và trả về round đã đồng bộ
        return roundRepository.saveAndFlush(saveRound);
    }
    @Override
    @Transactional
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
    public List<Round> findAllByEventId(Integer eventId) {
        List<Round> rounds = new ArrayList<>();
        rounds = roundRepository.findAllByHackathonEvent_EventId(eventId);
        return rounds;
    }

}
