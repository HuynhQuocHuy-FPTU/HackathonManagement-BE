package com.hackathon.service;

import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.auth.UpdateProfileRequest;
import com.hackathon.entity.Account;
import com.hackathon.exception.ApiException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AccountRepository accountRepository;

    @Override
    public AuthResponse getCurrentUser(CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .expiresIn(0)
                .accountId(account.getAccountId())
                .fullName(account.getAccountName())
                .email(account.getEmail())
                .role(account.getRole())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest request) {
        Account account = accountRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        // Cập nhật thông tin chung ở bảng Account
        account.setAccountName(request.getAccountName());
        account.setPhone(request.getPhone());

        // Phân nhánh cập nhật theo Role
        switch (account.getRole()) {
            case STUDENT:
                com.hackathon.entity.Student student = account.getStudent();
                if (student != null) {
                    student.setStudentName(request.getAccountName()); // Đồng bộ tên
                    if (request.getStudentCode() != null) student.setStudentCode(request.getStudentCode());
                    if (request.getAddress() != null) student.setAddress(request.getAddress());
                    if (request.getMajor() != null) student.setMajor(request.getMajor());
                }
                break;

            case EXPERT: // Hoặc JUDGE
                com.hackathon.entity.Expert expert = account.getExpert();
                if (expert != null) {
                    expert.setExpertName(request.getAccountName()); // Đồng bộ tên
                    if (request.getDepartment() != null) expert.setDepartment(request.getDepartment());
                    if (request.getWorkplace() != null) expert.setWorkplace(request.getWorkplace());
                }
                break;

            case EVENTCOORDINATOR:
                com.hackathon.entity.EventCoordinator coordinator = account.getEventCoordinator();
                if (coordinator != null) {
                    coordinator.setCoordinatorName(request.getAccountName()); // Đồng bộ tên
                    if (request.getDepartment() != null) coordinator.setDepartment(request.getDepartment());
                }
                break;

            default:
                break;
        }

        accountRepository.save(account);

        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .expiresIn(0)
                .accountId(account.getAccountId())
                .fullName(account.getAccountName())
                .email(account.getEmail())
                .role(account.getRole())
                .build();
    }
}