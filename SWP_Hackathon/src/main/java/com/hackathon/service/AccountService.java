package com.hackathon.service;

import com.hackathon.dto.student.StudentHistoryResponse;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface AccountService {
    StudentHistoryResponse studentHistory(Integer accountId, CustomUserDetails userDetails);
}
