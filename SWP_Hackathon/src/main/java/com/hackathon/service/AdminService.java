package com.hackathon.service;

import com.hackathon.dto.auth.InviteAccountRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.EventCoordinator;
import com.hackathon.entity.Expert;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.EventCoordinatorRepository;
import com.hackathon.repository.ExpertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ExpertRepository expertRepository;

    @Autowired
    private EventCoordinatorRepository eventCoordinatorRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

//    @Transactional(rollbackFor = Exception.class)
    // 💡 Đảm bảo an toàn dữ liệu, nếu gửi mail lỗi sẽ tự hủy bản ghi DB vừa tạo
//    public String inviteAccountByAdmin(InviteAccountRequest request) {
        // 1. Check trùng email dưới DB
//        if (accountRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
//            throw new BadRequestException("Email này đã được sử dụng trong hệ thống!");
//        }
//
//        // 2. Ép kiểu chuỗi Role từ request sang Enum bảo mật
//        AccountRole accountRole;
//        try {
//            accountRole = AccountRole.valueOf(request.getRole().toUpperCase());
//        } catch (IllegalArgumentException e) {
//            throw new BadRequestException("Role không hợp lệ! (EVENT_COORDINATOR, EXPERT)");
//        }
//
//        // 3. TỰ SINH MẬT KHẨU TẠM THỜI (Lấy 8 ký tự đầu từ chuỗi UUID ngẫu nhiên)
//        String temporaryPassword = UUID.randomUUID().toString().substring(0, 8);
//
//        // 4. KHỞI TẠO VÀ LƯU BẢNG ACCOUNT GỐC
//        Account account = new Account();
//        account.setEmail(request.getEmail().trim().toLowerCase());
//        account.setRole(accountRole);
//        account.setStatus(AccountStatus.ACTIVE); // Cho phép login luôn bằng mật khẩu tạm
//        account.setPasswordChanged(false);     //  Đánh dấu bắt buộc phải đổi mật khẩu ở lần đăng nhập đầu tiên
//        account.setPassword(passwordEncoder.encode(temporaryPassword)); // Băm mật khẩu lưu vào DB
//
//        Account savedAccount = accountRepository.save(account);
//
//        // 5. KHỞI TẠO CÁC BẢNG PHỤ NGHIỆP VỤ (Lưu kèm fullName)
//        if (accountRole == AccountRole.EVENTCOORDINATOR) {
//            EventCoordinator coordinator = new EventCoordinator();
//            coordinator.setAccount(savedAccount);
//            coordinator.setCoordinatorName(request.getFullName()); // Gán họ tên từ request vào đây
//            eventCoordinatorRepository.save(coordinator);
//        } else if (accountRole == AccountRole.EXPERT) {
//            Expert expert = new Expert();
//            expert.setAccount(savedAccount);
//            expert.setExpertName(request.getFullName());     // Gán họ tên từ request vào đây
//            expertRepository.save(expert);
//        }
//
//        // 6. GỌI EMAIL SERVICE ĐỂ GỬI MẬT KHẨU TẠM THỜI
//        emailService.sendTemporaryPasswordEmail(account.getEmail(), temporaryPassword, request.getFullName());
//
//        return "Đã tạo tài khoản và gửi mật khẩu tạm thời thành công đến: " + account.getEmail();
//    }


}
