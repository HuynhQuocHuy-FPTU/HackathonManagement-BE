package com.hackathon.service;

import com.hackathon.dto.auth.StudentUpdateRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.Student;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.entity.enums.StudentStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.StudentRepository;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl {
    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;

    public void completeRegister(CustomUserDetails userDetails, StudentUpdateRequest request) {
        Account account = accountRepository.findByEmail(userDetails.getAccount().getEmail())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản"));

        account.setPhone(request.getPhone());
        account = accountRepository.save(account);

        Student student = new Student();
        student.setStudentCode(request.getStudentCode());
        student.setStudentName(request.getStudentName());
        student.setAddress(request.getAddress());
        student.setUniversityName(request.getUniversity());
        student.setMajor(request.getMajor());
        student.setStartDate(LocalDateTime.now());
        student.setStatus(StudentStatus.STUDYING);
        student.setAccount(account);
        studentRepository.save(student);

    }

}
