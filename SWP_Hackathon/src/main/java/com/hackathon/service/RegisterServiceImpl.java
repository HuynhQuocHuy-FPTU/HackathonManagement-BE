package com.hackathon.service;

import com.hackathon.dto.auth.RegisterRequest;
import com.hackathon.dto.auth.ResendVerificationRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.Student;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.entity.enums.StudentStatus;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.StudentRepository;
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
public class RegisterServiceImpl implements RegisterService {

    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceImpl emailServiceImpl;

    @Value("${app.verification .expiration-hours:24}")
    private int verificationExpirationHours;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // Kiểm tra trùng lặp
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        if (accountRepository.existsByPhone(request.getPhone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng");
        }
        if (studentRepository.existsByStudentCode(request.getStudentCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "Mã sinh viên đã tồn tại");
        }

        String verificationToken = UUID.randomUUID().toString();

        Account account = new Account();
        account.setEmail(request.getEmail().trim().toLowerCase());
        account.setPhone(request.getPhone());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(AccountRole.STUDENT);
        account.setStatus(AccountStatus.INACTIVE);
        account.setVerificationToken(verificationToken);
        account.setVerificationTokenExpiry(LocalDateTime.now().plusHours(verificationExpirationHours));
        account = accountRepository.save(account);

        Student student = new Student();
        student.setStudentCode(request.getStudentCode());
        student.setStudentName(request.getStudentName());
        student.setAddress(request.getAddress());
        student.setMajor(request.getMajor());
        student.setStartDate(LocalDateTime.now());
        student.setStatus(StudentStatus.STUDYING);
        student.setAccount(account);
        studentRepository.save(student);
        emailServiceImpl.sendVerificationEmail(account.getEmail(), verificationToken);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        Account account = accountRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token xác thực không hợp lệ"));

        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản đã được xác thực");
        }
        if (account.getVerificationTokenExpiry() == null
                || account.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token xác thực đã hết hạn");
        }

        account.setStatus(AccountStatus.ACTIVE);
        account.setVerificationToken(null);
        account.setVerificationTokenExpiry(null);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        // Vẫn dùng email để gửi lại mã xác nhận là hợp lý
        Account account = accountRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tài khoản đã được xác thực");
        }
        if (account.getStatus() == AccountStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        String token = UUID.randomUUID().toString();
        account.setVerificationToken(token);
        account.setVerificationTokenExpiry(LocalDateTime.now().plusHours(verificationExpirationHours));
        accountRepository.save(account);
        emailServiceImpl.sendVerificationEmail(account.getEmail(), token);
    }

}