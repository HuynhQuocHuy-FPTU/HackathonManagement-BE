package com.hackathon.service;

import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.category.CategoryExpertAssignResponseDTO;
import com.hackathon.dto.category.CategoryRoundDTO;
import com.hackathon.dto.event.EventDTO;
import com.hackathon.dto.expert.ExpertAssginmentRequestDTO;
import com.hackathon.dto.expert.ExpertAssignmentResponseDTO;
import com.hackathon.dto.round.RoundDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.ExpertAssignRepository;
import com.hackathon.repository.ExpertRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertAssignServiceImpl implements ExpertAssignService {

    private final ExpertRepository expertRepository;
    private final AccountRepository accountRepository;
    private final ExpertAssignRepository expertAssignRepository;


    // =========================================================
    // ASSIGN EXPERTS
    // =========================================================

    @Override
    public void assignExpertsToCategoryRound(List<CategoryRound> saveCateRound,
                                             List<CategoryExpertAssignRequestDTO> requests,
                                             Round round) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        // 1. Gom tất cả expertId từ toàn bộ request — tránh N+1 query
        List<Integer> expertIds = requests.stream()
                .filter(r -> r.getExperts() != null)
                .flatMap(r -> r.getExperts().stream())
                .map(ExpertAssginmentRequestDTO::getExpertId)
                .distinct()
                .toList();

        Map<Integer, Expert> expertMap = expertRepository.findAllById(expertIds)
                .stream()
                .collect(Collectors.toMap(Expert::getExpertId, e -> e));

        // 2. Kích hoạt lại tài khoản INACTIVE (xử lý trên RAM, batch save cuối)
        List<Account> accountsToActivate = expertMap.values().stream()
                .map(Expert::getAccount)
                .filter(acc -> acc != null && acc.getStatus().equals(AccountStatus.INACTIVE))
                .toList();

        if (!accountsToActivate.isEmpty()) {
            accountsToActivate.forEach(acc -> acc.setStatus(AccountStatus.ACTIVE));
            accountRepository.saveAll(accountsToActivate);
        }

        // 3. Duyệt từng category trong request và tạo ExpertAssign
        List<ExpertAssign> allAssignments = new ArrayList<>();

        for (CategoryExpertAssignRequestDTO cateExpertAssign : requests) {
            if (cateExpertAssign.getExperts() == null || cateExpertAssign.getExperts().isEmpty()) {
                continue;
            }

            Integer index = cateExpertAssign.getCategoryId();
            if (index == null || index < 0 || index >= saveCateRound.size()) {
                throw new BadRequestException("Index category không hợp lệ hoặc không tồn tại: " + index);
            }
            CategoryRound cateRound = saveCateRound.get(index);

            for (var expertRequest : cateExpertAssign.getExperts()) {
                Expert expert = expertMap.get(expertRequest.getExpertId());
                if (expert == null) {
                    throw new BadRequestException("Không tìm thấy expert với id: " + expertRequest.getExpertId());
                }

                ExpertAssign assign = ExpertAssign.builder()
                        .categoryRound(cateRound)
                        .expert(expert)
                        .role(expertRequest.getRole())
                        .build();

                allAssignments.add(assign);
            }
        }

        // 4. Lưu tất cả assignment 1 lần duy nhất
        if (!allAssignments.isEmpty()) {
            expertAssignRepository.saveAll(allAssignments);
        }
    }

    // =========================================================
    // GET EXPERTS BY ROUND
    // =========================================================

    @Override
    public List<CategoryExpertAssignResponseDTO> getExpertAssignmentsByRound(Round round) {
        if (round == null) {
            return new ArrayList<>();
        }

        // 1. Lấy tất cả assignment thuộc round này
        List<ExpertAssign> assigns = expertAssignRepository
                .findByCategoryRound_Round_RoundId(round.getRoundId());

        if (assigns == null || assigns.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Group theo Category
        Map<Category, List<ExpertAssign>> groupByCategory = assigns.stream()
                .collect(Collectors.groupingBy(assign -> assign.getCategoryRound().getCategory()));

        // 3. Duyệt từng nhóm Category, chỉ lấy expert của đúng category đó
        return groupByCategory.entrySet().stream()
                .map(entry -> {
                    Category category = entry.getKey();

                    List<ExpertAssignmentResponseDTO> expertDTOs = entry.getValue().stream()
                            .map(assign -> ExpertAssignmentResponseDTO.builder()
                                    .expertId(assign.getExpert().getExpertId())
                                    .expertName(assign.getExpert().getExpertName())
                                    .role(assign.getRole())
                                    .build())
                            .toList();

                    return CategoryExpertAssignResponseDTO.builder()
                            .categoryId(category.getCategoryId())
                            .experts(expertDTOs)
                            .build();
                })
                .toList();
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void deleteByEventId(Integer eventId) {
        expertAssignRepository.deleteByEventId(eventId);
    }

    // =========================================================
    // GET
    // =========================================================
    @Override
    public List<EventDTO> getEventForJudge(CustomUserDetails userDetails) {
        int expertId = userDetails.getAccount().getExpert().getExpertId();

        List<HackathonEvent> eventList = expertAssignRepository.findEventByJudge(expertId, List.of(ExpertRole.CORE_JUDGE, ExpertRole.GUEST_JUDGE));

        return eventList.stream().map(e -> new EventDTO(e.getEventId(), e.getEventName())).toList();
    }

    @Override
    public List<RoundDTO> getRoundForJudge(CustomUserDetails userDetails, Integer eventId) {
        int expertId = userDetails.getAccount().getExpert().getExpertId();

        List<Round> roundList = expertAssignRepository.findRoundByJudge(eventId, expertId, List.of(ExpertRole.CORE_JUDGE, ExpertRole.GUEST_JUDGE));

        return roundList.stream().map(r -> new RoundDTO(r.getRoundId(), r.getRoundName())).toList();
    }

    @Override
    public List<CategoryRoundDTO> getCategoryRoundForJudge(CustomUserDetails userDetails, Integer roundId) {
        int expertId = userDetails.getAccount().getExpert().getExpertId();

        List<CategoryRound> categoryRoundList = expertAssignRepository.findCategoryByJudge(roundId, expertId, List.of(ExpertRole.CORE_JUDGE, ExpertRole.GUEST_JUDGE));

        return categoryRoundList.stream().map(cr -> new CategoryRoundDTO(cr.getCategoryRoundId(), cr.getCategory().getCategoryName())).toList();
    }


}