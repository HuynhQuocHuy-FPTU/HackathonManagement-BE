package com.hackathon.service.impl;

import com.hackathon.dto.auth.RegisterRequest;
import com.hackathon.dto.auth.ResendVerificationRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.Student;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.StudentRepository;
import com.hackathon.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
// Xử lý đăng ký tài khoản, xác minh thư điện tử và gửi lại mã xác minh.
public class RegisterServiceImpl implements RegisterService {

    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceImpl emailServiceImpl;

    // Số giờ mã xác minh có hiệu lực, mặc định là 24 giờ nếu chưa cấu hình.
    @Value("${app.verification .expiration-hours:24}")
    private int verificationExpirationHours;

    @Override
    @Transactional
    // Kiểm tra thông tin, mã hóa mật khẩu và tạo tài khoản chưa xác minh.
    public void register(RegisterRequest request) {
        // Không cho phép đăng ký nếu thư điện tử đã thuộc một tài khoản khác.
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        // Không cho phép dùng lại số điện thoại đã được lưu trong hệ thống.
        if (accountRepository.existsByPhone(request.getPhone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng");
        }
        // Không cho phép tạo hồ sơ mới bằng mã sinh viên đã tồn tại.
        if (studentRepository.existsByStudentCode(request.getStudentCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "Mã sinh viên đã tồn tại");
        }

        // Tạo mã ngẫu nhiên để xác minh quyền sở hữu thư điện tử.
        String verificationToken = UUID.randomUUID().toString();

        // Khởi tạo tài khoản từ thông tin đăng ký đã vượt qua các kiểm tra trùng lặp.
        Account account = new Account();
        // Chuẩn hóa thư điện tử bằng cách bỏ khoảng trắng và chuyển về chữ thường.
        account.setEmail(request.getEmail().trim().toLowerCase());
        // Lưu số điện thoại dùng để liên hệ và xác định tài khoản.
        account.setPhone(request.getPhone());
        // Chỉ lưu mật khẩu đã mã hóa, không lưu mật khẩu gốc.
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        // Người tự đăng ký vào hệ thống được gán vai trò sinh viên.
        account.setRole(AccountRole.STUDENT);
        // Giữ tài khoản ở trạng thái chưa hoạt động cho đến khi xác minh thư điện tử.
        account.setStatus(AccountStatus.INACTIVE);
        // Gắn mã xác minh vừa tạo vào tài khoản.
        account.setVerificationToken(verificationToken);
        // Thiết lập thời điểm hết hạn dựa trên cấu hình số giờ hiệu lực.
        account.setVerificationTokenExpiry(LocalDateTime.now().plusHours(verificationExpirationHours));
        // Đánh dấu người dùng đã tự thiết lập mật khẩu khi đăng ký.
        account.setPasswordChanged(true);
        // Lưu tài khoản trước để lấy bản ghi dùng liên kết với hồ sơ sinh viên.
        account = accountRepository.save(account);

        // Tạo hồ sơ sinh viên tương ứng với tài khoản vừa được lưu.
        Student student = new Student();
        // Sao chép lần lượt thông tin học tập và thông tin cá nhân từ yêu cầu đăng ký.
        student.setStudentCode(request.getStudentCode());
        student.setStudentName(request.getStudentName());
        student.setAddress(request.getAddress());
        student.setUniversityName(request.getUniversity());
        student.setMajor(request.getMajor());
        // Thiết lập quan hệ giữa hồ sơ sinh viên và tài khoản.
        student.setAccount(account);
        // Lưu hồ sơ sinh viên sau khi đã gán đủ thông tin.
        studentRepository.save(student);
        // Gửi mã xác minh đến thư điện tử để người dùng kích hoạt tài khoản.
        emailServiceImpl.sendVerificationEmail(account.getEmail(), verificationToken);
    }

    @Override
    @Transactional
    // Xác minh mã gửi qua thư và kích hoạt tài khoản nếu mã còn hiệu lực.
    public void verifyEmail(String token) {
        // Tìm đúng tài khoản đang sở hữu mã xác minh được gửi lên.
        Account account = accountRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token xác thực không hợp lệ"));

        // Dừng xử lý nếu tài khoản đã được kích hoạt trước đó.
        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản đã được xác thực");
        }
        // Mã không có thời hạn hoặc đã quá hạn đều không được chấp nhận.
        if (account.getVerificationTokenExpiry() == null
                || account.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token xác thực đã hết hạn");
        }

        // Kích hoạt tài khoản sau khi mã vượt qua toàn bộ kiểm tra.
        account.setStatus(AccountStatus.ACTIVE);
        // Xóa mã đã dùng để mã đó không thể được sử dụng lại.
        account.setVerificationToken(null);
        // Xóa thời điểm hết hạn vì tài khoản không còn mã xác minh đang hoạt động.
        account.setVerificationTokenExpiry(null);
        // Lưu trạng thái kích hoạt và thông tin mã đã được làm sạch.
        accountRepository.save(account);
    }

    @Override
    @Transactional
    // Tạo mã xác minh mới và gửi lại khi tài khoản vẫn chưa được kích hoạt.
    public void resendVerification(ResendVerificationRequest request) {
        // Chuẩn hóa thư điện tử rồi tìm tài khoản cần gửi lại mã xác minh.
        Account account = accountRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        // Tài khoản đã hoạt động không cần nhận thêm mã xác minh.
        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản đã được xác thực");
        }
        // Không gửi mã mới cho tài khoản đang bị khóa.
        if (account.getStatus() == AccountStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        // Sinh mã mới để thay thế hoàn toàn mã xác minh cũ.
        String token = UUID.randomUUID().toString();
        // Gắn mã mới vào tài khoản chưa được kích hoạt.
        account.setVerificationToken(token);
        // Tính lại thời điểm hết hạn bắt đầu từ lần gửi lại này.
        account.setVerificationTokenExpiry(LocalDateTime.now().plusHours(verificationExpirationHours));
        // Lưu mã mới trước khi gửi thư để mã trong thư luôn khớp dữ liệu hệ thống.
        accountRepository.save(account);
        // Gửi mã xác minh mới đến đúng thư điện tử của tài khoản.
        emailServiceImpl.sendVerificationEmail(account.getEmail(), token);
    }

}
