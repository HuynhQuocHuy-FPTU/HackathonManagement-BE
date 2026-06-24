package com.hackathon.service.user;

import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.user.UpdateProfileRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp triển khai các nghiệp vụ liên quan đến quản lý thông tin Người dùng.
 * Chịu trách nhiệm đồng bộ dữ liệu giữa bảng Account cốt lõi và các bảng định danh chi tiết (Student, Expert, EventCoordinator).
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AccountRepository accountRepository;

    /**
     * Lấy thông tin hồ sơ chi tiết của người dùng đang đăng nhập hiện tại.
     * Tự động nhận diện Role và trích xuất các trường thông tin tương ứng từ các bảng con.
     *
     * @param userDetails Đối tượng chứa thông tin xác thực (Principal) từ Spring Security Context
     * @return AuthResponse Payload chứa đầy đủ thông tin cá nhân (không cấp mới token)
     */
    @Override
    public AuthResponse getCurrentUser(CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();

        // 1. Sử dụng Custom Query để lấy Full Name từ các bảng con (Tối ưu hóa Database Normalization)
        String fullName = accountRepository.findFullNameByEmail(account.getEmail()).orElse(null);

        // 2. Trích xuất tên trường Đại học (Chỉ áp dụng nếu Role là Sinh viên)
        String university = (account.getRole() == AccountRole.STUDENT && account.getStudent() != null)
                ? account.getStudent().getUniversityName() : null;

        // 3. Trích xuất tên Tổ chức/Đơn vị công tác (Chỉ áp dụng cho Giám khảo hoặc Ban tổ chức)
        String organization = null;
        if (account.getRole() == AccountRole.EXPERT && account.getExpert() != null) {
            organization = account.getExpert().getOrganization();
        } else if (account.getRole() == AccountRole.EVENTCOORDINATOR && account.getEventCoordinator() != null) {
            organization = account.getEventCoordinator().getOrganization();
        }

        // Đóng gói dữ liệu trả về cho Frontend hiển thị Profile
        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .expiresIn(0)
                .accountId(account.getAccountId())
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
     * Áp dụng kỹ thuật Role-based data extraction để ngăn chặn hoàn toàn lỗ hổng Mass Assignment.
     *
     * @param userDetails Context người dùng hiện tại đang thực hiện request
     * @param request     Unified DTO chứa toàn bộ các trường cập nhật có thể có từ Frontend
     * @return AuthResponse chứa thông tin Profile đã được làm mới
     */
    @Override
    @Transactional // Đảm bảo tính toàn vẹn dữ liệu: Cập nhật Account và bảng con phải cùng thành công hoặc cùng thất bại
    public AuthResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest request) {
        Account account = accountRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        // Cập nhật thông tin dùng chung ở bảng cốt lõi
        account.setPhone(request.getPhone());

        String university = null;
        String organization = null;

        // BỨC TƯỜNG LỬA BẢO MẬT (Security Firewall):
        // Chỉ map (gán) các trường dữ liệu được phép dựa trên Role thực tế của Account dưới Database.
        // Mọi trường dữ liệu rác/vượt quyền do Frontend cố tình gửi lên sẽ bị rẽ nhánh này bỏ qua hoàn toàn.
        switch (account.getRole()) {
            case STUDENT:
                com.hackathon.entity.Student student = account.getStudent();
                if (student != null) {
                    student.setStudentName(request.getUserName()); // Đồng bộ tên xuống bảng Student
                    if (request.getStudentCode() != null) student.setStudentCode(request.getStudentCode());
                    if (request.getAddress() != null) student.setAddress(request.getAddress());
                    if (request.getMajor() != null) student.setMajor(request.getMajor());

                    if (request.getUniversityName() != null) student.setUniversityName(request.getUniversityName());
                    university = student.getUniversityName();
                }
                break;

            case EXPERT:
                com.hackathon.entity.Expert expert = account.getExpert();
                if (expert != null) {
                    expert.setExpertName(request.getUserName()); // Đồng bộ tên xuống bảng Expert
                    if (request.getDepartment() != null) expert.setDepartment(request.getDepartment());
                    if (request.getWorkplace() != null) expert.setWorkplace(request.getWorkplace());

                    if (request.getOrganization() != null) expert.setOrganization(request.getOrganization());
                    organization = expert.getOrganization();
                }
                break;

            case EVENTCOORDINATOR:
                com.hackathon.entity.EventCoordinator coordinator = account.getEventCoordinator();
                if (coordinator != null) {
                    coordinator.setCoordinatorName(request.getUserName()); // Đồng bộ tên xuống bảng EventCoordinator
                    if (request.getDepartment() != null) coordinator.setDepartment(request.getDepartment());

                    if (request.getOrganization() != null) coordinator.setOrganization(request.getOrganization());
                    organization = coordinator.getOrganization();
                }
                break;

            default:
                // Đối với các Role hệ thống (như ADMIN) không có bảng con, bỏ qua xử lý
                break;
        }

        // Nhờ cơ chế CascadeType.ALL cấu hình trên Entity Account,
        // Hibernate sẽ tự động sinh câu lệnh UPDATE cho cả bảng Account và bảng con tương ứng.
        accountRepository.save(account);

        // Trả về thông tin vừa được cập nhật để Frontend đồng bộ State (như Redux/Context)
        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .expiresIn(0)
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
}