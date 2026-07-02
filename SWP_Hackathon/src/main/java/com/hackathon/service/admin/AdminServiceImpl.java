package com.hackathon.service.admin;

import com.hackathon.dto.UserAdminResponse;
import com.hackathon.dto.admin.InviteAccountRequest;
import com.hackathon.dto.admin.UpdateAccountStatusRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.EventCoordinator;
import com.hackathon.entity.Expert;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.exception.ApiException;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.EventCoordinatorRepository;
import com.hackathon.repository.ExpertRepository;
import com.hackathon.repository.RefreshTokenRepository;
import com.hackathon.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AccountRepository accountRepository;
    private final ExpertRepository expertRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public List<UserAdminResponse> getAllUsers() {
        // Lấy toàn bộ danh sách Account từ Database
        List<Account> accounts = accountRepository.findAll();

        // Map từng Entity Account sang DTO UserAdminResponse
        return accounts.stream()
                .map(this::mapToUserAdminResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserAdminResponse getUserById(int id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));

        return mapToUserAdminResponse(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteAccount(InviteAccountRequest request) {
        // 1. Kiểm tra Role hợp lệ
        if (request.getRole() != AccountRole.EXPERT && request.getRole() != AccountRole.EVENTCOORDINATOR) {
            throw new BadRequestException("Chỉ được phép tạo tài khoản cho EXPERT hoặc EVENTCOORDINATOR");
        }

        // 2. Kiểm tra trùng Email
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (accountRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BadRequestException("Email này đã được sử dụng trong hệ thống!");
        }

        // 3. Tự sinh mật khẩu tạm thời (8 ký tự đầu của UUID)
        String temporaryPassword = UUID.randomUUID().toString().substring(0, 8);

        // 4. Khởi tạo Account gốc
        Account account = new Account();
        account.setEmail(normalizedEmail);
        account.setRole(request.getRole());
        account.setStatus(AccountStatus.ACTIVE);
        account.setPassword(passwordEncoder.encode(temporaryPassword));

        // CỜ BẢO MẬT: Đánh dấu bắt buộc phải đổi mật khẩu và update profile ở lần đăng nhập đầu tiên
        account.setPasswordChanged(false);

        Account savedAccount = accountRepository.save(account);

        // 5. Khởi tạo các bảng phụ (Expert / EventCoordinator)
        if (request.getRole() == AccountRole.EVENTCOORDINATOR) {
            EventCoordinator coordinator = new EventCoordinator();
            coordinator.setAccount(savedAccount);
            coordinator.setCoordinatorName(request.getFullName());
            eventCoordinatorRepository.save(coordinator);
        } else if (request.getRole() == AccountRole.EXPERT) {
            Expert expert = new Expert();
            expert.setAccount(savedAccount);
            expert.setExpertName(request.getFullName());
            expertRepository.save(expert);
        }

        // 6. Gửi Email thông báo (Hàm này bạn đã viết sẵn rất tốt trong EmailServiceImpl)
        emailService.sendTemporaryPasswordEmail(savedAccount.getEmail(), temporaryPassword, request.getFullName());
    }

    /**
     * Hàm Helper: Đóng gói Entity Account thành DTO trả về cho Admin.
     * Xử lý trích xuất dữ liệu đa quyền (Role-based data extraction) tương tự như luồng Profile.
     */
    private UserAdminResponse mapToUserAdminResponse(Account account) {
        // 1. Lấy Full Name
        String fullName = accountRepository.findFullNameByEmail(account.getEmail()).orElse(null);

        // 2. Lấy University (Nếu là Sinh viên)
        String university = (account.getRole() == AccountRole.STUDENT && account.getStudent() != null)
                ? account.getStudent().getUniversityName() : null;

        // 3. Lấy Organization (Nếu là Giám khảo hoặc Ban tổ chức)
        String organization = null;
        if (account.getRole() == AccountRole.EXPERT && account.getExpert() != null) {
            organization = account.getExpert().getOrganization();
        } else if (account.getRole() == AccountRole.EVENTCOORDINATOR && account.getEventCoordinator() != null) {
            organization = account.getEventCoordinator().getOrganization();
        }

        return UserAdminResponse.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .phone(account.getPhone())
                .fullName(fullName)
                .role(account.getRole())
                .status(account.getStatus())
                .avatarUrl(account.getAvatarUrl())
                .university(university)
                .organization(organization)
                .createdAt(account.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(int accountId, UpdateAccountStatusRequest request) {
        // 1. Tìm tài khoản
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + accountId));

        // 2. Không cho phép tự khóa chính mình (Optional: Nếu bạn có truyền UserDetails của Admin đang đăng nhập vào, hãy so sánh ID)

        // 3. Cập nhật trạng thái mới
        account.setStatus(request.getStatus());
        accountRepository.save(account);

        // 4. BẢO MẬT: Nếu Admin "Khóa" (BANNED) hoặc "Vô hiệu hóa" (INACTIVE) tài khoản
        // -> Lập tức thu hồi toàn bộ Token để văng session hiện tại của họ
        if (request.getStatus() == AccountStatus.BANNED || request.getStatus() == AccountStatus.INACTIVE) {
            refreshTokenRepository.revokeAllByAccount(account);
        }
    }

}
