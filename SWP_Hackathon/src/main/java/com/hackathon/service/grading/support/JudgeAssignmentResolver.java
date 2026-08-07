package com.hackathon.service.grading.support;

import com.hackathon.entity.Account;
import com.hackathon.entity.Expert;
import com.hackathon.entity.ExpertAssign;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.ExpertAssignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Trách nhiệm: Giải quyết và kiểm tra bối cảnh phân công công việc của Chuyên gia.
 * Đảm bảo nguyên tắc Single Responsibility (SRP): Cô lập logic kiểm tra vai trò.
 */
@Component
@RequiredArgsConstructor
public class JudgeAssignmentResolver {

    private final ExpertAssignRepository expertAssignRepository;

    // Danh sách các vai trò có thẩm quyền thực thi chấm điểm
    private static final List<ExpertRole> VALID_JUDGE_ROLES = List.of(ExpertRole.CORE_JUDGE, ExpertRole.GUEST_JUDGE);

    /**
     * Xác thực thực thể hồ sơ Expert từ tài khoản đăng nhập hệ thống.
     */
    public Expert resolveExpert(Account account) {
        if (account.getExpert() == null) {
            throw new BadRequestException("Tài khoản hiện tại không tồn tại hồ sơ dữ liệu Chuyên gia (Expert).");
        }
        return account.getExpert();
    }

    /**
     * Xác thực quyền chấm điểm động dựa trên phân công (ExpertAssign) tại hạng mục tương ứng.
     * Ngăn chặn nghiêm ngặt trường hợp Mentor thực hiện chấm điểm.
     */
    public ExpertAssign requireJudgeAssignment(Expert expert, Integer categoryRoundId) {
        return expertAssignRepository.findJudgeAssignment(categoryRoundId, expert.getExpertId(), VALID_JUDGE_ROLES)
                .orElseThrow(() -> {
                    // Kiểm tra ngữ cảnh lỗi nếu người dùng là Mentor để đưa ra phản hồi chính xác nhất
                    boolean isMentor = expertAssignRepository.findMentorAssignment(categoryRoundId, expert.getExpertId()).isPresent();
                    if (isMentor) {
                        return new BadRequestException("Tài khoản của bạn được phân công làm MENTOR tại hạng mục này. " +
                                "Theo quy định nghiệp vụ, Mentor không có quyền chấm điểm ứng viên.");
                    }
                    return new BadRequestException("Bạn không được Ban tổ chức phân công chấm điểm tại hạng mục/vòng thi này.");
                });
    }
}
