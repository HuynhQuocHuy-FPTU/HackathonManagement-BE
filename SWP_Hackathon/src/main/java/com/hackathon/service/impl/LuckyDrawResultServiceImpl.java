package com.hackathon.service.impl;

import com.hackathon.dto.DrawResponseDTO;
import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.RegistrationStatus;
import com.hackathon.entity.enums.WorkshopStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.LuckyDrawResultService;
import com.hackathon.service.NotificationService;
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
// Xử lý việc nhập, cập nhật và truy xuất kết quả phân đội vào các danh mục bằng bốc thăm.
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
        // Lấy tài khoản ban tổ chức để làm người gửi thông báo kết quả cho các đội.
        Account acc = userDetails.getAccount();

        // Tải sự kiện cùng dữ liệu đăng ký cần thiết cho quá trình nhập kết quả.
        HackathonEvent event = eventRepository.findByIdForRegistrationApproval(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        // Chỉ cho phép nhập kết quả trước thời điểm sự kiện chính thức bắt đầu.
        validateDrawResultTime(event);

        // Bốc thăm chỉ được ghi nhận sau khi workshop đã hoàn thành.
        if (event.getWorkshopStatus() != WorkshopStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể gán kết quả bốc thăm sau khi Workshop đã hoàn thành!");
        }

        // Yêu cầu phải chứa ít nhất một nhóm kết quả bốc thăm.
        if (drawResults == null || drawResults.isEmpty()) {
            throw new BadRequestException("Danh sách kết quả bốc thăm không được rỗng");
        }

        // Không cho phép dùng chức năng nhập lần đầu nếu sự kiện đã có kết quả trước đó.
        validateNotAlreadyImported(eventId);
        // Kiểm tra các danh mục và đăng ký được phân bổ đầy đủ, không trùng lặp.
        validateDrawResultDistribution(eventId, drawResults);

        // Kết quả bốc thăm luôn được gắn vào cấu hình danh mục của vòng đầu tiên.
        Round firstRound = roundRepository.findFirstByHackathonEvent_EventIdOrderByOrderIndexAsc(eventId)
                .orElseThrow(() -> new BadRequestException("Event " + event.getEventName() + " chưa có round nào"));

        // Thu thập những lần tham gia đã được cập nhật để trả về sau khi hoàn tất.
        List<TeamParticipant> updateTeamParticipants = new ArrayList<>();

        // Xử lý lần lượt từng danh mục cùng danh sách đăng ký được bốc vào danh mục đó.
        for (DrawResultRequestDTO drawResult : drawResults) {

            // Lấy mã danh mục đang được xử lý trong nhóm kết quả hiện tại.
            Integer categoryId = drawResult.getCategoryId();

            // Xác nhận danh mục tồn tại và thuộc đúng sự kiện đang nhập kết quả.
            Category category = categoryRepository.findCategoryByCategoryIdAndHackathonEvent_EventId(categoryId, eventId).orElseThrow(() -> new BadRequestException("Không tìm thấy category: " + categoryId + " với eventID: " + eventId));

            // Tìm cấu hình liên kết giữa danh mục này và vòng đầu tiên.
            CategoryRound categoryRound = categoryRoundRepository.findCategoryRoundByCategory_CategoryIdAndRound_RoundId(categoryId, firstRound.getRoundId())
                    .orElseThrow(() -> new BadRequestException("Chưa có CategoryRound cho category " + categoryId + " ở round đầu tiên"));

            // Gán lần lượt từng đăng ký trong nhóm vào cùng danh mục vòng.
            for (Integer registrationId : drawResult.getRegistrationId()) {

                // Tìm đăng ký và bảo đảm đăng ký đó thuộc đúng sự kiện.
                Registration registration = registrationRepository.findRegistrationByRegistrationIdAndHackathonEvent_EventId(registrationId, eventId)
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy registration: " + registrationId + " thuộc event: " + eventId));

                // Chỉ đội có đăng ký đã được duyệt mới được đưa vào kết quả bốc thăm.
                if (registration.getStatus() != RegistrationStatus.APPROVED) {
                    // Trả thông báo riêng khi đăng ký đã bị ban tổ chức từ chối.
                    if(registration.getStatus() == RegistrationStatus.REJECTED){
                        throw new BadRequestException("Registration của đội " + registration.getTeam().getTeamName() + " đã bị từ chối");
                    }else{
                        throw new BadRequestException("Registration của đội" + registration.getTeam().getTeamName() + " chưa được chấp nhận");
                    }
                }

                // Tìm lần tham gia được tạo từ đăng ký đã duyệt.
                TeamParticipant teamParticipant = participantRepository.findParticipantByRegistration_RegistrationId(registration.getRegistrationId())
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy participant theo registration id " + registrationId));

                // Chức năng nhập lần đầu không được ghi đè đội đã có danh mục.
                if (teamParticipant.getCategoryRound() != null) {
                    throw new BadRequestException("Registration " + registrationId + " đã được gán vào Category rồi");
                }

                // Gắn đội vào danh mục của vòng đầu tiên theo kết quả bốc thăm.
                teamParticipant.setCategoryRound(categoryRound);
                // Lưu thay đổi của lần tham gia ngay sau khi gán.
                teamParticipant = participantRepository.save(teamParticipant);
                // Ghi nhận đối tượng đã cập nhật vào danh sách kết quả.
                updateTeamParticipants.add(teamParticipant);

                // Lấy đội từ đăng ký để xác định trưởng nhóm nhận thông báo.
                Team team = teamParticipant.getRegistration().getTeam();
                // Tìm tài khoản của thành viên đang giữ vai trò trưởng nhóm.
                Account accountLeader = team.getTeamMembers()
                        .stream()
                        .filter(TeamMember::getIsLeader)
                        .map(TeamMember::getStudent)
                        .map(Student::getAccount)
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy trưởng nhóm của team " + team.getTeamName()));

                // Gửi tên danh mục vừa bốc được và hạn phản hồi đến trưởng nhóm.
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

    private void validateNotAlreadyImported(Integer eventId) {
        // Kiểm tra sự kiện đã có ít nhất một đội được gán danh mục hay chưa.
        boolean hasImportedDrawResults = !participantRepository
                .findAllByRegistration_HackathonEvent_EventIdAndCategoryRoundIsNotNull(eventId)
                .isEmpty();
        // Nếu đã có dữ liệu thì phải dùng chức năng cập nhật để tránh nhập chồng kết quả.
        if (hasImportedDrawResults) {
            throw new BadRequestException(
                    "Kết quả bốc thăm đã được import; hãy sử dụng chức năng cập nhật"
            );
        }
    }

    private void validateDrawResultDistribution(
            Integer eventId,
            List<DrawResultRequestDTO> drawResults
    ) {
        // Kiểm tra mỗi danh mục và mỗi đăng ký chỉ xuất hiện đúng một lần.
        validateUniqueCategoryAssignments(drawResults);

        // Lấy toàn bộ đăng ký hợp lệ bắt buộc phải xuất hiện trong kết quả nhập.
        List<Registration> approvedRegistrations = registrationRepository
                .findByHackathonEvent_EventIdAndStatus(
                        eventId,
                        RegistrationStatus.APPROVED
                );
        // Lấy toàn bộ danh mục của sự kiện để kiểm tra phạm vi và mức phân bổ tối thiểu.
        List<Category> categories =
                categoryRepository.findAllByHackathonEvent_EventId(eventId);

        // Không thể nhập kết quả khi sự kiện chưa cấu hình danh mục.
        if (categories.isEmpty()) {
            throw new BadRequestException(
                    "Event chưa có category để nhập kết quả bốc thăm"
            );
        }

        // Tạo tập mã danh mục hợp lệ thuộc sự kiện để tra cứu nhanh.
        Set<Integer> eventCategoryIds = categories.stream()
                .map(Category::getCategoryId)
                .collect(Collectors.toSet());
        // Lưu số đội được nhập theo từng danh mục để kiểm tra mức phân bổ.
        Map<Integer, Integer> importedCountByCategory = new LinkedHashMap<>();
        // Gom tất cả mã đăng ký được nhập để đối chiếu với danh sách đã duyệt.
        List<Integer> importedRegistrationIds = new ArrayList<>();

        for (DrawResultRequestDTO drawResult : drawResults) {
            // Kiểm tra danh mục trong dữ liệu nhập thật sự thuộc sự kiện.
            Integer categoryId = drawResult.getCategoryId();
            if (!eventCategoryIds.contains(categoryId)) {
                throw new BadRequestException(
                        "Category " + categoryId + " không thuộc event " + eventId
                );
            }

            // Ghi nhận số đăng ký được phân vào danh mục hiện tại.
            importedCountByCategory.put(
                    categoryId,
                    drawResult.getRegistrationId().size()
            );
            // Thêm các đăng ký của danh mục vào danh sách đối chiếu chung.
            importedRegistrationIds.addAll(drawResult.getRegistrationId());
        }

        // Tổng số mã được nhập phải bằng tổng số đăng ký đã được duyệt.
        if (importedRegistrationIds.size() != approvedRegistrations.size()) {
            throw new BadRequestException(
                    "Tổng số registration import phải bằng số registration đã APPROVED: "
                            + approvedRegistrations.size()
            );
        }

        // Chuyển sang tập hợp để phát hiện một đăng ký bị lặp ở nhiều vị trí.
        Set<Integer> uniqueImportedRegistrationIds =
                new HashSet<>(importedRegistrationIds);
        // Kích thước thay đổi sau khi tạo tập hợp chứng tỏ dữ liệu có mã trùng.
        if (uniqueImportedRegistrationIds.size()
                != importedRegistrationIds.size()) {
            throw new BadRequestException(
                    "Một registration không được xuất hiện nhiều lần trong kết quả bốc thăm"
            );
        }

        // Tạo tập mã đăng ký hợp lệ được lấy trực tiếp từ cơ sở dữ liệu.
        Set<Integer> approvedRegistrationIds = approvedRegistrations.stream()
                .map(Registration::getRegistrationId)
                .collect(Collectors.toSet());
        // Hai tập phải giống hệt nhau để không thiếu đội hoặc chứa đội ngoài danh sách duyệt.
        if (!uniqueImportedRegistrationIds.equals(approvedRegistrationIds)) {
            throw new BadRequestException(
                    "Danh sách import phải bao gồm chính xác tất cả registration đã APPROVED"
            );
        }

        // Tính số đội tối thiểu mỗi danh mục phải nhận để bảo đảm phân bổ tương đối đều.
        int minimumPerCategory =
                approvedRegistrations.size() / categories.size();
        // Ghi lại các danh mục chưa đạt mức tối thiểu để trả lỗi đầy đủ một lần.
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

        // Từ chối toàn bộ kết quả khi còn ít nhất một danh mục chưa đủ đội.
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
    // Cập nhật lại danh mục của các đội khi kết quả bốc thăm cần điều chỉnh trước sự kiện.
    public List<TeamParticipant> updateDrawResults(
            Integer eventId,
            List<DrawResultRequestDTO> drawResults,
            CustomUserDetails userDetails
    ) {
        // Thu thập những đội thật sự đổi danh mục trong lần cập nhật này.
        List<TeamParticipant> updatedParticipants = new ArrayList<>();

        // Tìm sự kiện cần điều chỉnh kết quả bốc thăm.
        HackathonEvent event = eventRepository.findByIdForRegistrationApproval(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        // Xác định vòng đầu tiên vì kết quả phân danh mục được áp dụng tại vòng này.
        Round firstRound = roundRepository.findFirstByHackathonEvent_EventIdOrderByOrderIndexAsc(eventId)
                .orElseThrow(() -> new BadRequestException("Event " + eventId + " chưa có round nào"));

        // Không cho phép sửa kết quả sau khi sự kiện đã bắt đầu.
        validateDrawResultTime(event);
        // Kiểm tra lại toàn bộ dữ liệu phân bổ trước khi thay đổi bất kỳ đội nào.
        validateDrawResultDistribution(eventId, drawResults);

        for (DrawResultRequestDTO dto : drawResults) {
            // Lấy mã danh mục đích của nhóm cập nhật hiện tại.
            Integer categoryId = dto.getCategoryId();

            // Tìm cấu hình danh mục đích trong vòng đầu tiên.
            CategoryRound targetCategoryRound = categoryRoundRepository.findCategoryRoundByCategory_CategoryIdAndRound_RoundId(categoryId, firstRound.getRoundId()).orElseThrow(() -> new BadRequestException("Không tìm thấy CategoryRound cho category ID: " + categoryId));

            for (Integer regId : dto.getRegistrationId()) {
                // Tìm đăng ký theo mã và xác nhận đăng ký thuộc đúng sự kiện.
                Registration registration = registrationRepository.findRegistrationByRegistrationIdAndHackathonEvent_EventId(regId, eventId)
                        .orElseThrow(() -> new BadRequestException("Registration " + regId + " không thuộc sự kiện này"));

                // Không cho phép đưa đăng ký chưa được duyệt vào danh mục thi đấu.
                if (registration.getStatus() != RegistrationStatus.APPROVED) {
                    throw new BadRequestException("Đội " + registration.getTeam().getTeamName() + " chưa được APPROVED, không thể cập nhật hạng mục.");
                }

                // Tìm lần tham gia đang lưu kết quả danh mục của đăng ký.
                TeamParticipant participant = participantRepository.findParticipantByRegistration_RegistrationId(regId)
                        .orElseThrow(() -> new BadRequestException("Không tìm thấy participant cho registration ID: " + regId));

                // Chỉ ghi dữ liệu khi danh mục mới khác danh mục hiện tại của đội.
                boolean isCategoryDifferent = participant.getCategoryRound() == null
                        || !Objects.equals(
                                participant.getCategoryRound().getCategoryRoundId(),
                                targetCategoryRound.getCategoryRoundId()
                        );

                if (isCategoryDifferent) {
                    // Ghi nhận tên cũ và tên mới để hỗ trợ theo dõi thay đổi khi cần.
                    String oldCategoryName = (participant.getCategoryRound() != null)
                            ? participant.getCategoryRound().getCategory().getCategoryName() : "Chưa có";
                    String newCategoryName = targetCategoryRound.getCategory().getCategoryName();

                    // Thay danh mục hiện tại bằng danh mục đích đã được kiểm tra.
                    participant.setCategoryRound(targetCategoryRound);
                    // Lưu kết quả điều chỉnh của đội.
                    participant = participantRepository.save(participant);
                    // Chỉ thêm đội có thay đổi thật sự vào kết quả trả về.
                    updatedParticipants.add(participant);

                }
            }
        }
        return updatedParticipants;
    }

    private void validateUniqueCategoryAssignments(
            List<DrawResultRequestDTO> drawResults
    ) {
        // Danh sách rỗng không chứa đủ thông tin để kiểm tra hoặc nhập kết quả.
        if (drawResults == null || drawResults.isEmpty()) {
            throw new BadRequestException(
                    "Danh sách kết quả bốc thăm không được rỗng"
            );
        }

        // Theo dõi các danh mục đã gặp để ngăn một danh mục xuất hiện nhiều nhóm.
        Set<Integer> categoryIds = new HashSet<>();
        // Theo dõi các đăng ký đã gặp để ngăn một đội được phân vào nhiều danh mục.
        Set<Integer> registrationIds = new HashSet<>();

        for (DrawResultRequestDTO drawResult : drawResults) {
            // Mỗi nhóm phải có mã danh mục và ít nhất một mã đăng ký hợp lệ.
            if (drawResult == null
                    || drawResult.getCategoryId() == null
                    || drawResult.getRegistrationId() == null
                    || drawResult.getRegistrationId().isEmpty()) {
                throw new BadRequestException(
                        "Mỗi category phải có danh sách registration hợp lệ"
                );
            }

            // Phép thêm trả về false khi mã danh mục đã tồn tại trong tập hợp.
            if (!categoryIds.add(drawResult.getCategoryId())) {
                throw new BadRequestException(
                        "Mỗi category chỉ được xuất hiện một lần"
                );
            }

            for (Integer registrationId : drawResult.getRegistrationId()) {
                // Mã đăng ký rỗng không thể xác định đội cần phân danh mục.
                if (registrationId == null) {
                    throw new BadRequestException(
                            "Registration ID không được để trống"
                    );
                }
                // Phép thêm trả về false khi đăng ký đã thuộc một nhóm trước đó.
                if (!registrationIds.add(registrationId)) {
                    throw new BadRequestException(
                            "Mỗi registration chỉ được thuộc một category"
                    );
                }
            }
        }
    }

    private void validateDrawResultTime(HackathonEvent event) {
        // Sự kiện phải có thời gian bắt đầu để xác định giới hạn được phép chỉnh bốc thăm.
        if (event.getStartDate() == null) {
            throw new BadRequestException(
                    "Sự kiện chưa cấu hình thời gian bắt đầu"
            );
        }

        // Tại hoặc sau thời điểm bắt đầu sự kiện, kết quả bốc thăm không còn được thay đổi.
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
        // Xác nhận sự kiện tồn tại trước khi truy vấn các kết quả đã gán.
        eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy event"));

        // Lấy các đội thuộc sự kiện đã có danh mục sau khi bốc thăm.
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
