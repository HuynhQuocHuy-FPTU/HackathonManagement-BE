package com.hackathon.service.impl;

import com.hackathon.dto.auth.StudentUpdateRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.Student;
import com.hackathon.exception.ApiException;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.StudentRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// Hoàn thiện thông tin hồ sơ sinh viên sau khi tài khoản được tạo.
public class StudentServiceImpl {
    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;

    // Kiểm tra tài khoản rồi cập nhật các thông tin sinh viên còn thiếu trong lần đăng ký đầu.
    public void completeRegister(CustomUserDetails userDetails, StudentUpdateRequest request) {
        // Dùng thư điện tử trong phiên đăng nhập để lấy lại tài khoản mới nhất từ cơ sở dữ liệu.
        Account account = accountRepository.findByEmail(userDetails.getAccount().getEmail())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản"));

        // Số điện thoại phải là duy nhất để tránh nhiều tài khoản dùng chung thông tin liên hệ.
        if (accountRepository.existsByPhone(request.getPhone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng");
        }

        // Mỗi mã sinh viên chỉ được phép xuất hiện trong một hồ sơ sinh viên.
        if (studentRepository.existsByStudentCode(request.getStudentCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "Mã sinh viên đã tồn tại");
        }

        // Cập nhật số điện thoại thuộc phần thông tin chung của tài khoản.
        account.setPhone(request.getPhone());
        // Cập nhật đường dẫn ảnh đại diện do sinh viên cung cấp.
        account.setAvatarUrl(request.getAvatar());
        // Lưu tài khoản trước để hồ sơ sinh viên liên kết với bản ghi đã được cập nhật.
        account = accountRepository.save(account);

        // Khởi tạo hồ sơ sinh viên mới cho tài khoản vừa hoàn thiện đăng ký.
        Student student = new Student();
        // Gán mã sinh viên đã được kiểm tra không trùng lặp.
        student.setStudentCode(request.getStudentCode());
        // Gán họ tên sinh viên từ dữ liệu người dùng gửi lên.
        student.setStudentName(request.getStudentName());
        // Gán địa chỉ liên hệ của sinh viên.
        student.setAddress(request.getAddress());
        // Gán tên trường đại học đang theo học.
        student.setUniversityName(request.getUniversity());
        // Gán chuyên ngành hiện tại của sinh viên.
        student.setMajor(request.getMajor());
        // Liên kết hồ sơ sinh viên với tài khoản tương ứng.
        student.setAccount(account);
        // Lưu hồ sơ để hoàn tất quá trình bổ sung thông tin sinh viên.
        studentRepository.save(student);

    }

}
