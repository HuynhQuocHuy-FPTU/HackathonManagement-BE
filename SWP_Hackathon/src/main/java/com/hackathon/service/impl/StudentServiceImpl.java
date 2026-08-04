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
public class StudentServiceImpl {
    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;

    public void completeRegister(CustomUserDetails userDetails, StudentUpdateRequest request) {
        Account account = accountRepository.findByEmail(userDetails.getAccount().getEmail())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản"));
        if (accountRepository.existsByPhone(request.getPhone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng");
        }
        if (studentRepository.existsByStudentCode(request.getStudentCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "Mã sinh viên đã tồn tại");
        }

        account.setPhone(request.getPhone());
        account.setAvatarUrl(request.getAvatar());
        account = accountRepository.save(account);

        Student student = new Student();
        student.setStudentCode(request.getStudentCode());
        student.setStudentName(request.getStudentName());
        student.setAddress(request.getAddress());
        student.setUniversityName(request.getUniversity());
        student.setMajor(request.getMajor());
        student.setAccount(account);
        studentRepository.save(student);

    }

}
