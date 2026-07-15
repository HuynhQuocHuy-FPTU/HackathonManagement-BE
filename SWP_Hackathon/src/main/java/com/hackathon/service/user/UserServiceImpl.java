package com.hackathon.service.user;

import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.user.UpdateProfileRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.ExpertRepository;
import com.hackathon.repository.StudentRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;
    private final ExpertRepository expertRepository;

    @Override
    public AuthResponse getCurrentUser(CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();

        // 1. Lấy Full Name (tối ưu hóa Database Normalization)
        String fullName = accountRepository.findFullNameByEmail(account.getEmail()).orElse(null);

        // 2. Trích xuất tên trường Đại học (Chỉ sinh viên mới có)
        String university = (account.getRole() == AccountRole.STUDENT && account.getStudent() != null)
                ? account.getStudent().getUniversityName() : null;

        // 3. Trích xuất tên Tổ chức (Chỉ Giám khảo hoặc Ban tổ chức mới có)
        String organization = null;
        if (account.getRole() == AccountRole.EXPERT && account.getExpert() != null) {
            organization = account.getExpert().getOrganization();
        } else if (account.getRole() == AccountRole.EVENTCOORDINATOR && account.getEventCoordinator() != null) {
            organization = account.getEventCoordinator().getOrganization();
        }

        return AuthResponse.builder()
                .accountId(account.getAccountId())
                .githubUsername(account.getGithubUsername())
                .githubId(account.getGithubId())
                .fullName(fullName)
                .email(account.getEmail())
                .role(account.getRole())
                .avatarUrl(account.getAvatarUrl())
                .university(university)
                .organization(organization)
                .createdAt(account.getCreatedAt())
                .accountStatus(account.getStatus())
                .build();
    }

    /**
     * Xử lý luồng cập nhật hồ sơ người dùng đa quyền (Multi-role Profile Update).
     * Áp dụng nguyên tắc SRP (Đơn trách nhiệm) để bóc tách logic theo từng Role.
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // Đảm bảo tính toàn vẹn: Lỗi ở bảng con thì bảng cha cũng Rollback
    public AuthResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest request) {
        Account account = accountRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        // 1. Cập nhật thông tin dùng chung ở bảng cốt lõi (Account)
        account.setPhone(request.getPhone());

        String university = null;
        String organization = null;

        // 2. ĐIỀU HƯỚNG LOGIC (Router)
        // Dựa vào Role, hệ thống gọi đúng hàm xử lý tương ứng. Bỏ qua các data rác từ Frontend.
        switch (account.getRole()) {
            case STUDENT:
                university = updateStudentProfile(account, request);
                break;
            case EXPERT:
                organization = updateExpertProfile(account, request);
                break;
            case EVENTCOORDINATOR:
                organization = updateCoordinatorProfile(account, request);
                break;
            default:
                break;
        }

        // 3. Nhờ cơ chế CascadeType.ALL, lệnh save(account) tự động UPDATE cả bảng con (Student/Expert...)
        accountRepository.save(account);

        // 4. Trả về state mới cho Frontend
        return AuthResponse.builder()
                .accountId(account.getAccountId())
                .fullName(request.getUserName())
                .email(account.getEmail())
                .role(account.getRole())
                .avatarUrl(account.getAvatarUrl())
                .university(university)
                .organization(organization)
                .createdAt(account.getCreatedAt())
                .accountStatus(account.getStatus())
                .build();
    }

    // =========================================================================
    // CÁC HÀM XỬ LÝ PRIVATE (Tuân thủ nguyên tắc SRP và OCP)
    // =========================================================================

    private String updateStudentProfile(Account account, UpdateProfileRequest request) {
        com.hackathon.entity.Student student = account.getStudent();
        if (student == null) return null;

        // BỨC TƯỜNG LỬA (Firewall): Chặn cập nhật nếu sinh viên đang trong giải đấu (Đang thi hoặc Đã qua vòng)
        boolean isCompeting = studentRepository.isParticipatingInOngoingCompetition(
                student.getStudentId(),
                List.of(ParticipantStatus.ACTIVE, ParticipantStatus.PASSED)
        );

        if (isCompeting) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Hồ sơ đã bị khóa để đảm bảo tính công bằng vì bạn đang trong thời gian tham gia cuộc thi.");
        }

        // Thực hiện cập nhật
        student.setStudentName(request.getUserName());
        if (request.getStudentCode() != null) student.setStudentCode(request.getStudentCode());
        if (request.getAddress() != null) student.setAddress(request.getAddress());
        if (request.getMajor() != null) student.setMajor(request.getMajor());
        if (request.getUniversityName() != null) student.setUniversityName(request.getUniversityName());

        return student.getUniversityName();
    }

    private String updateExpertProfile(Account account, UpdateProfileRequest request) {
        com.hackathon.entity.Expert expert = account.getExpert();
        if (expert == null) return null;

        // BỨC TƯỜNG LỬA (Firewall): Chặn cập nhật nếu BGK đang được phân công sự kiện (ACTIVE hoặc ONGOING)
        boolean isAssignedToOngoingEvent = expertRepository.isAssignedToOngoingEvent(
                expert.getExpertId(),
                List.of(EventStatus.ACTIVE, EventStatus.ONGOING)
        );

        if (isAssignedToOngoingEvent) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Hồ sơ đã bị khóa vì bạn đang được phân công làm nhiệm vụ trong một cuộc thi đang diễn ra.");
        }

        // Thực hiện cập nhật
        expert.setExpertName(request.getUserName());
        if (request.getDepartment() != null) expert.setDepartment(request.getDepartment());
        if (request.getOrganization() != null) expert.setOrganization(request.getOrganization());

        return expert.getOrganization();
    }

    private String updateCoordinatorProfile(Account account, UpdateProfileRequest request) {
        com.hackathon.entity.EventCoordinator coordinator = account.getEventCoordinator();
        if (coordinator == null) return null;

        // Ban tổ chức (Coordinator) thường được tự do cập nhật thông tin hơn
        coordinator.setCoordinatorName(request.getUserName());
        if (request.getDepartment() != null) coordinator.setDepartment(request.getDepartment());
        if (request.getOrganization() != null) coordinator.setOrganization(request.getOrganization());

        return coordinator.getOrganization();
    }
}