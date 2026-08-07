package com.hackathon.service.impl;

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
import com.hackathon.service.ExpertAssignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// Quản lý việc phân công chuyên gia vào từng danh mục của vòng thi.
public class ExpertAssignServiceImpl implements ExpertAssignService {

    private final ExpertRepository expertRepository;
    private final AccountRepository accountRepository;
    private final ExpertAssignRepository expertAssignRepository;


    // Tạo toàn bộ phân công chuyên gia cho các danh mục thuộc một vòng thi.
    @Override
    public void assignExpertsToCategoryRound(List<CategoryRound> saveCateRound,
                                             List<CategoryExpertAssignRequestDTO> requests,
                                             Round round) {
        // Không có yêu cầu phân công thì kết thúc mà không tác động dữ liệu hiện tại.
        if (requests == null || requests.isEmpty()) {
            return;
        }

        // Gom mã chuyên gia từ tất cả danh mục và loại bỏ mã bị lặp trước khi truy vấn.
        List<Integer> expertIds = requests.stream()
                .filter(r -> r.getExperts() != null)
                .flatMap(r -> r.getExperts().stream())
                .map(ExpertAssginmentRequestDTO::getExpertId)
                .distinct()
                .toList();

        // Tải toàn bộ chuyên gia trong một lần và lập bảng tra cứu theo mã chuyên gia.
        Map<Integer, Expert> expertMap = expertRepository.findAllById(expertIds)
                .stream()
                .collect(Collectors.toMap(Expert::getExpertId, e -> e));

        // Lọc các tài khoản chuyên gia đang chưa hoạt động để kích hoạt lại khi được phân công.
        List<Account> accountsToActivate = expertMap.values().stream()
                .map(Expert::getAccount)
                .filter(acc -> acc != null && acc.getStatus().equals(AccountStatus.INACTIVE))
                .toList();

        // Chỉ thực hiện cập nhật cơ sở dữ liệu khi có ít nhất một tài khoản cần kích hoạt.
        if (!accountsToActivate.isEmpty()) {
            // Chuyển trạng thái từng tài khoản chuyên gia sang hoạt động.
            accountsToActivate.forEach(acc -> acc.setStatus(AccountStatus.ACTIVE));
            // Lưu toàn bộ tài khoản một lần để giảm số lần truy cập cơ sở dữ liệu.
            accountRepository.saveAll(accountsToActivate);
        }

        // Chuẩn bị danh sách chứa mọi phân công hợp lệ sẽ được lưu sau khi kiểm tra xong.
        List<ExpertAssign> allAssignments = new ArrayList<>();

        // Xử lý lần lượt yêu cầu phân công của từng danh mục.
        for (CategoryExpertAssignRequestDTO cateExpertAssign : requests) {
            // Bỏ qua danh mục không có chuyên gia nào được chọn.
            if (cateExpertAssign.getExperts() == null || cateExpertAssign.getExperts().isEmpty()) {
                continue;
            }

            // Giá trị categoryId hiện được dùng làm vị trí danh mục trong danh sách đã lưu.
            Integer index = cateExpertAssign.getCategoryId();
            // Vị trí phải nằm trong giới hạn danh sách danh mục của vòng.
            if (index == null || index < 0 || index >= saveCateRound.size()) {
                throw new BadRequestException("Index category không hợp lệ hoặc không tồn tại: " + index);
            }
            // Lấy danh mục vòng tương ứng với vị trí đã được kiểm tra.
            CategoryRound cateRound = saveCateRound.get(index);

            // Tạo một bản ghi phân công riêng cho từng chuyên gia của danh mục.
            for (var expertRequest : cateExpertAssign.getExperts()) {
                // Tra cứu chuyên gia từ bảng đã tải trước đó thay vì truy vấn lại cơ sở dữ liệu.
                Expert expert = expertMap.get(expertRequest.getExpertId());
                // Mã không có trong bảng tra cứu nghĩa là chuyên gia không tồn tại.
                if (expert == null) {
                    throw new BadRequestException("Không tìm thấy expert với id: " + expertRequest.getExpertId());
                }

                // Tạo quan hệ giữa chuyên gia, danh mục vòng và vai trò được giao.
                ExpertAssign assign = ExpertAssign.builder()
                        .categoryRound(cateRound)
                        .expert(expert)
                        .role(expertRequest.getRole())
                        .build();

                // Thêm phân công hợp lệ vào danh sách chờ lưu.
                allAssignments.add(assign);
            }
        }

        // Chỉ gọi thao tác lưu khi danh sách có ít nhất một phân công mới.
        if (!allAssignments.isEmpty()) {
            // Lưu toàn bộ phân công trong một lần để bảo đảm hiệu quả xử lý.
            expertAssignRepository.saveAll(allAssignments);
        }
    }

    @Override
    public List<CategoryExpertAssignResponseDTO> getExpertAssignmentsByRound(Round round) {
        if (round == null) {
            return new ArrayList<>();
        }

        List<ExpertAssign> assigns = expertAssignRepository
                .findByCategoryRound_Round_RoundId(round.getRoundId());

        if (assigns == null || assigns.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Category, List<ExpertAssign>> groupByCategory = assigns.stream()
                .collect(Collectors.groupingBy(assign -> assign.getCategoryRound().getCategory()));

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

    // Xóa toàn bộ phân công chuyên gia thuộc các vòng của một sự kiện.
    @Override
    public void deleteByEventId(Integer eventId) {
        // Repository thực hiện xóa theo mã sự kiện trên tất cả danh mục vòng liên quan.
        expertAssignRepository.deleteByEventId(eventId);
    }

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
