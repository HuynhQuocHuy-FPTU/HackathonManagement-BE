package com.hackathon.service;

import com.hackathon.dto.DrawResponseDTO;
import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.entity.enums.WorkshopStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class LuckyDrawResultServiceImpl implements LuckyDrawResultService {
    private final CategoryRepository categoryRepository;
    private final RoundRepository roundRepository;
    private final CategoryRoundRepository categoryRoundRepository;
    private final ParticipantRepository participantRepository;
    private final RegistrationRepository registrationRepository;
    private final HackathonEventRepository eventRepository;
    private final NotificationService notificationService;
    @Transactional
    @Override
    public List<TeamParticipant> importDrawResults(Integer eventId, List<DrawResultRequestDTO> drawResults, CustomUserDetails userDetails, Integer responseDeadline) {
        Account acc = userDetails.getAccount();

        // Tìm event
        HackathonEvent event = eventRepository.findByIdForRegistrationApproval(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        validateDrawResultTime(event);

        if (event.getWorkshopStatus() != WorkshopStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể gán kết quả bốc thăm sau khi Workshop đã hoàn thành!");
        }

        if (drawResults == null || drawResults.isEmpty()) {
            throw new BadRequestException("Danh sách kết quả bốc thăm không được rỗng");
        }

        // Tìm round đầu tiên
        validateImportDistribution(eventId, drawResults);

        Round firstRound = roundRepository.findFirstByHackathonEvent_EventIdOrderByOrderIndexAsc(eventId)
                .orElseThrow(() -> new BadRequestException("Event " + event.getEventName() + " chưa có round nào"));

        List<TeamParticipant> updateTeamParticipants = new ArrayList<>();

        // Vòng lặp chính: Duyệt qua từng nhóm kết quả (Category + List Registration)
        for (DrawResultRequestDTO drawResult : drawResults) {

            Integer categoryId = drawResult.getCategoryId();

            // 2. Lấy category, thuộc đúng event (làm 1 lần ngoài vòng lặp registration)
            Category category = categoryRepository.findCategoryByCategoryIdAndHackathonEvent_EventId(categoryId, eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy category: " + categoryId + " với eventID: " + eventId));

            // 4. Tìm category round ở round đầu tiên (làm 1 lần ngoài vòng lặp registration)
            CategoryRound categoryRound = categoryRoundRepository.findCategoryRoundByCategory_CategoryIdAndRound_RoundId(categoryId, firstRound.getRoundId())
                    .orElseThrow(() -> new BadRequestException("Chưa có CategoryRound cho category " + categoryId + " ở round đầu tiên"));

            // Duyệt qua từng registrationId trong category đó
            for (Integer registrationId : drawResult.getRegistrationId()) {

                // 1. Lấy registration
                Registration registration = registrationRepository.findRegistrationByRegistrationIdAndHackathonEvent_EventId(registrationId, eventId)
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy registration: " + registrationId + " thuộc event: " + eventId));

                if (registration.getStatus() != RegistrationStatus.APPROVED) {
                    if(registration.getStatus() == RegistrationStatus.REJECTED){
                        throw new BadRequestException("Registration của đội " + registration.getTeam().getTeamName() + " đã bị từ chối");
                    }else{
                        throw new BadRequestException("Registration của đội" + registration.getTeam().getTeamName() + " chưa được chấp nhận");
                    }
                }

                // 3. Lấy participant
                TeamParticipant teamParticipant = participantRepository.findParticipantByRegistration_RegistrationId(registration.getRegistrationId())
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy participant theo registration id " + registrationId));

                if (teamParticipant.getCategoryRound() != null) {
                    throw new BadRequestException("Registration " + registrationId + " đã được gán vào Category rồi");
                }

                // 5. Cập nhật Participant
                teamParticipant.setCategoryRound(categoryRound);
                teamParticipant = participantRepository.save(teamParticipant);
                updateTeamParticipants.add(teamParticipant);

                // 6. Thông báo
                Team team = teamParticipant.getRegistration().getTeam();
                Account accountLeader = team.getTeamMembers()
                        .stream()
                        .filter(TeamMember::getIsLeader)
                        .map(TeamMember::getStudent)
                        .map(Student::getAccount)
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy trưởng nhóm của team " + team.getTeamName()));

                notificationService.notifyAssignedCategory(
                        acc,
                        accountLeader,
                        team,
                        firstRound,
                        event.getEventName(),
                        category.getCategoryName(),
                        responseDeadline,
                        ""
                );
            }
        }

        return updateTeamParticipants;
    }

    private void validateImportDistribution(
            Integer eventId,
            List<DrawResultRequestDTO> drawResults
    ) {
        validateUniqueCategoryAssignments(drawResults);

        List<Registration> approvedRegistrations = registrationRepository
                .findByHackathonEvent_EventIdAndStatus(
                        eventId,
                        RegistrationStatus.APPROVED
                );
        List<Category> categories =
                categoryRepository.findAllByHackathonEvent_EventId(eventId);

        boolean hasImportedDrawResults = !participantRepository
                .findAllByRegistration_HackathonEvent_EventIdAndCategoryRoundIsNotNull(eventId)
                .isEmpty();
        if (hasImportedDrawResults) {
            throw new BadRequestException(
                    "Kết quả bốc thăm đã được import; hãy sử dụng chức năng cập nhật"
            );
        }

        if (categories.isEmpty()) {
            throw new BadRequestException(
                    "Event chưa có category để nhập kết quả bốc thăm"
            );
        }

        Set<Integer> eventCategoryIds = categories.stream()
                .map(Category::getCategoryId)
                .collect(Collectors.toSet());
        Map<Integer, Integer> importedCountByCategory = new LinkedHashMap<>();
        List<Integer> importedRegistrationIds = new ArrayList<>();

        for (DrawResultRequestDTO drawResult : drawResults) {
            Integer categoryId = drawResult.getCategoryId();
            if (!eventCategoryIds.contains(categoryId)) {
                throw new BadRequestException(
                        "Category " + categoryId + " không thuộc event " + eventId
                );
            }

            importedCountByCategory.put(
                    categoryId,
                    drawResult.getRegistrationId().size()
            );
            importedRegistrationIds.addAll(drawResult.getRegistrationId());
        }

        if (importedRegistrationIds.size() != approvedRegistrations.size()) {
            throw new BadRequestException(
                    "Tổng số registration import phải bằng số registration đã APPROVED: "
                            + approvedRegistrations.size()
            );
        }

        Set<Integer> uniqueImportedRegistrationIds =
                new HashSet<>(importedRegistrationIds);
        if (uniqueImportedRegistrationIds.size()
                != importedRegistrationIds.size()) {
            throw new BadRequestException(
                    "Một registration không được xuất hiện nhiều lần trong kết quả bốc thăm"
            );
        }

        Set<Integer> approvedRegistrationIds = approvedRegistrations.stream()
                .map(Registration::getRegistrationId)
                .collect(Collectors.toSet());
        if (!uniqueImportedRegistrationIds.equals(approvedRegistrationIds)) {
            throw new BadRequestException(
                    "Danh sách import phải bao gồm chính xác tất cả registration đã APPROVED"
            );
        }

        int minimumPerCategory =
                approvedRegistrations.size() / categories.size();
        List<String> insufficientCategories = new ArrayList<>();
        for (Category category : categories) {
            int importedCount = importedCountByCategory.getOrDefault(
                    category.getCategoryId(),
                    0
            );
            if (importedCount < minimumPerCategory) {
                insufficientCategories.add(
                        category.getCategoryName() + ": "
                                + importedCount + "/" + minimumPerCategory
                );
            }
        }

        if (!insufficientCategories.isEmpty()) {
            throw new BadRequestException(
                    "Kết quả bốc thăm chưa được phân bổ đủ cho tất cả category. "
                            + "Có " + approvedRegistrations.size()
                            + " registration APPROVED và " + categories.size()
                            + " category, nên mỗi category phải có ít nhất "
                            + minimumPerCategory + " registration. "
                            + "Các category chưa đủ: "
                            + String.join(", ", insufficientCategories)
            );
        }
    }

    @Transactional
    @Override
    public List<TeamParticipant> updateDrawResults(
            Integer eventId,
            List<DrawResultRequestDTO> drawResults,
            CustomUserDetails userDetails
    ) {
        List<TeamParticipant> updatedParticipants = new ArrayList<>();

        // Tìm event
        HackathonEvent event = eventRepository.findByIdForRegistrationApproval(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        validateUniqueCategoryAssignments(drawResults);

        // Tìm round đầu tiên
        Round firstRound = roundRepository.findFirstByHackathonEvent_EventIdOrderByOrderIndexAsc(eventId)
                .orElseThrow(() -> new BadRequestException("Event " + eventId + " chưa có round nào"));

        validateDrawResultTime(event);

        for (DrawResultRequestDTO dto : drawResults) {
            Integer categoryId = dto.getCategoryId();

            // 1. Tìm CategoryRound đích (để biết đội sẽ được gán vào đâu)
            CategoryRound targetCategoryRound = categoryRoundRepository.findCategoryRoundByCategory_CategoryIdAndRound_RoundId(categoryId, firstRound.getRoundId()).orElseThrow(() -> new BadRequestException("Không tìm thấy CategoryRound cho category ID: " + categoryId));

            for (Integer regId : dto.getRegistrationId()) {
                // 2. REGISTRATION
                Registration registration = registrationRepository.findRegistrationByRegistrationIdAndHackathonEvent_EventId(regId, eventId)
                        .orElseThrow(() -> new BadRequestException("Registration " + regId + " không thuộc sự kiện này"));

                if (registration.getStatus() != RegistrationStatus.APPROVED) {
                    throw new BadRequestException("Đội " + registration.getTeam().getTeamName() + " chưa được APPROVED, không thể cập nhật hạng mục.");
                }

                // Lấy Participant tương ứng
                TeamParticipant participant = participantRepository.findParticipantByRegistration_RegistrationId(regId)
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy participant cho registration ID: " + regId));

                // 3. SO SÁNH ĐỂ TỐI ƯU (Tránh thông báo thừa)
                // Chỉ cập nhật nếu Category hiện tại khác với Target
                boolean isCategoryDifferent = participant.getCategoryRound() == null
                        || !Objects.equals(
                                participant.getCategoryRound().getCategoryRoundId(),
                                targetCategoryRound.getCategoryRoundId()
                        );

                if (isCategoryDifferent) {
                    String oldCategoryName = (participant.getCategoryRound() != null)
                            ? participant.getCategoryRound().getCategory().getCategoryName() : "Chưa có";
                    String newCategoryName = targetCategoryRound.getCategory().getCategoryName();

                    // Thực hiện cập nhật
                    participant.setCategoryRound(targetCategoryRound);
                    participant = participantRepository.save(participant);
                    updatedParticipants.add(participant);

                }
            }
        }
        return updatedParticipants;
    }

    private void validateUniqueCategoryAssignments(
            List<DrawResultRequestDTO> drawResults
    ) {
        if (drawResults == null || drawResults.isEmpty()) {
            throw new BadRequestException(
                    "Danh sách kết quả bốc thăm không được rỗng"
            );
        }

        Set<Integer> categoryIds = new HashSet<>();
        Set<Integer> registrationIds = new HashSet<>();

        for (DrawResultRequestDTO drawResult : drawResults) {
            if (drawResult == null
                    || drawResult.getCategoryId() == null
                    || drawResult.getRegistrationId() == null
                    || drawResult.getRegistrationId().isEmpty()) {
                throw new BadRequestException(
                        "Mỗi category phải có danh sách registration hợp lệ"
                );
            }

            if (!categoryIds.add(drawResult.getCategoryId())) {
                throw new BadRequestException(
                        "Mỗi category chỉ được xuất hiện một lần"
                );
            }

            for (Integer registrationId : drawResult.getRegistrationId()) {
                if (registrationId == null) {
                    throw new BadRequestException(
                            "Registration ID không được để trống"
                    );
                }
                if (!registrationIds.add(registrationId)) {
                    throw new BadRequestException(
                            "Mỗi registration chỉ được thuộc một category"
                    );
                }
            }
        }
    }

    private void validateDrawResultTime(HackathonEvent event) {
        if (event.getStartDate() == null) {
            throw new BadRequestException(
                    "Sự kiện chưa cấu hình thời gian bắt đầu"
            );
        }

        if (!LocalDateTime.now().isBefore(event.getStartDate())) {
            throw new BadRequestException(
                    "Không thể nhập hoặc cập nhật kết quả bốc thăm "
                            + "sau khi sự kiện đã bắt đầu"
            );
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<DrawResponseDTO> getDrawResults(
            Integer eventId,
            CustomUserDetails userDetails
    ) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        List<TeamParticipant> participants = participantRepository
                .findAllByRegistration_HackathonEvent_EventIdAndCategoryRoundIsNotNull(eventId);

        Map<Category, List<Integer>> registrationIdsByCategory = participants.stream()
                .collect(Collectors.groupingBy(
                        participant -> participant.getCategoryRound().getCategory(),
                        LinkedHashMap::new,
                        Collectors.mapping(participant -> participant.getRegistration().getRegistrationId(), Collectors.toList())
                ));

        return registrationIdsByCategory.entrySet().stream()
                .map(entry -> DrawResponseDTO.builder()
                        .categoryId(entry.getKey().getCategoryId())
                        .categoryName(entry.getKey().getCategoryName())
                        .registrationId(entry.getValue())
                        .build())
                .toList();
    }
}
