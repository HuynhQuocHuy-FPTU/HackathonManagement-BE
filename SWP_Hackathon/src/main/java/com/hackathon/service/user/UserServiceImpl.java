package com.hackathon.service.user;

import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.user.UpdateProfileRequest;
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

        // Gọi hàm custom query để lấy Full Name từ các bảng con
        String fullName = accountRepository.findFullNameByEmail(account.getEmail()).orElse(null);

        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .expiresIn(0)
                .accountId(account.getAccountId())
                .fullName(fullName) // <-- Sử dụng fullName
                .email(account.getEmail())
                .role(account.getRole())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest request) {
        Account account = accountRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        // 1. Bảng Account giờ chỉ còn chứa các thông tin chung như phone (Không còn accountName)
        account.setPhone(request.getPhone());

        // 2. Phân nhánh cập nhật TÊN và các thông tin khác trực tiếp vào bảng con
        switch (account.getRole()) {
            case STUDENT:
                com.hackathon.entity.Student student = account.getStudent();
                if (student != null) {
                    student.setStudentName(request.getUserName()); // Cập nhật tên thẳng vào bảng Student
                    if (request.getStudentCode() != null) student.setStudentCode(request.getStudentCode());
                    if (request.getAddress() != null) student.setAddress(request.getAddress());
                    if (request.getMajor() != null) student.setMajor(request.getMajor());
                }
                break;

            case EXPERT: // Hoặc JUDGE
                com.hackathon.entity.Expert expert = account.getExpert();
                if (expert != null) {
                    expert.setExpertName(request.getUserName()); // Cập nhật tên thẳng vào bảng Expert
                    if (request.getDepartment() != null) expert.setDepartment(request.getDepartment());
                    if (request.getWorkplace() != null) expert.setWorkplace(request.getWorkplace());
                }
                break;

            case EVENTCOORDINATOR: // Chú ý Role enum của bạn viết liền
                com.hackathon.entity.EventCoordinator coordinator = account.getEventCoordinator();
                if (coordinator != null) {
                    coordinator.setCoordinatorName(request.getUserName()); // Cập nhật tên thẳng vào bảng EventCoordinator
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
                .fullName(request.getUserName()) // Trả về luôn tên vừa cập nhật từ Request cho UI hiển thị
                .email(account.getEmail())
                .role(account.getRole())
                .build();
    }
}