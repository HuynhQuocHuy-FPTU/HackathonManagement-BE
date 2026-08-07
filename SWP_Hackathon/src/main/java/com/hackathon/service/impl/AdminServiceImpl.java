package com.hackathon.service.impl;

import com.hackathon.dto.AdminOverviewResponse;
import com.hackathon.dto.AuditLogResponse;
import com.hackathon.dto.UserAdminResponse;
import com.hackathon.dto.admin.InviteAccountRequest;
import com.hackathon.dto.admin.UpdateAccountStatusRequest;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.exception.ApiException;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.service.AdminService;
import com.hackathon.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// Cung cấp các nghiệp vụ quản trị tài khoản và số liệu tổng quan dành cho quản trị viên.
public class AdminServiceImpl implements AdminService {

    private final AccountRepository accountRepository;
    private final ExpertRepository expertRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // Lấy toàn bộ tài khoản và chuyển từng tài khoản sang dữ liệu hiển thị cho quản trị viên.
    @Override
    public List<UserAdminResponse> getAllUsers() {
        // Lấy toàn bộ danh sách Account từ Database
        List<Account> accounts = accountRepository.findAll();

        return accounts.stream()
                .map(this::mapToUserAdminResponse)
                .collect(Collectors.toList());
    }

    // Tìm một tài khoản theo mã và trả về đầy đủ thông tin theo vai trò của tài khoản đó.
    @Override
    public UserAdminResponse getUserById(int id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));

        return mapToUserAdminResponse(account);
    }

    // Tạo tài khoản được mời, gán đúng vai trò và gửi thư chứa thông tin đăng nhập ban đầu.
    // Toàn bộ thao tác được thực hiện trong một giao dịch để tránh lưu dữ liệu dở dang.
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

    private UserAdminResponse mapToUserAdminResponse(Account account) {
        String fullName = accountRepository.findFullNameByEmail(account.getEmail()).orElse(null);

        String university = (account.getRole() == AccountRole.STUDENT && account.getStudent() != null)
                ? account.getStudent().getUniversityName() : null;

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

    // Cập nhật trạng thái hoạt động của tài khoản sau khi kiểm tra tài khoản có tồn tại.
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

    // Đếm và tổng hợp các số liệu chính của hệ thống để hiển thị trên trang quản trị.
    @Override
    public AdminOverviewResponse getOverviewForAdmin() {

        long totalRoles = AccountRole.values().length;
        long highLevelAccounts = accountRepository.countByRole(AccountRole.EXPERT)
                + accountRepository.countByRole(AccountRole.EVENTCOORDINATOR);
        LocalDateTime time = LocalDateTime.now().minusHours(24);
        long totalLogs24h = auditLogRepository.countTotalLogs24h(time);
        long bannedAccounts = accountRepository.countByStatus(AccountStatus.BANNED)
                + accountRepository.countByStatus(AccountStatus.INACTIVE);

        // Metrics
        AdminOverviewResponse.AdminMetricsResponse metrics = new AdminOverviewResponse.AdminMetricsResponse();
        metrics.setSystemRoles(totalRoles);
        metrics.setTotalLog24h(totalLogs24h);
        metrics.setHighLevelAccounts(highLevelAccounts);
        metrics.setBannedAccounts(bannedAccounts);
        //Role distribution
        long studentCount = accountRepository.countByRole(AccountRole.STUDENT);
        long adminCount = accountRepository.countByRole(AccountRole.ADMIN);
        long coordinatorCount = accountRepository.countByRole(AccountRole.EVENTCOORDINATOR);
        long expertCount = accountRepository.countByRole(AccountRole.EXPERT);
        long totalUser = studentCount + adminCount + coordinatorCount + expertCount;
        AdminOverviewResponse.RoleDistributionResponse distribution = new AdminOverviewResponse.RoleDistributionResponse();
        distribution.setStudentCount(studentCount);
        distribution.setAdminCount(adminCount);
        distribution.setCoordinatorCount(coordinatorCount);
        distribution.setExpertCount(expertCount);
        distribution.setTotalUsers(totalUser);
        //RecentAuditLogs
        List<AuditLog> list = auditLogRepository.findTop10ByOrderByCreatedAtDesc();
        List<AuditLogResponse> recentLogs = new ArrayList<>();
        for (AuditLog log : list) {
            AuditLogResponse res = new AuditLogResponse();
            res.setId(log.getId());
            res.setAccountId(log.getAccount().getAccountId());
            res.setAction(log.getAction().name());
            res.setRole(log.getAccount().getRole());
            res.setCreatedAt(log.getCreatedAt());
            res.setActorName(log.getActorName());
//            res.setData();
            recentLogs.add(res);
        }
        return new AdminOverviewResponse(
                metrics,
                distribution,
                recentLogs
        );


    }



}
