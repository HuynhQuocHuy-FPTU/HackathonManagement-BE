package com.hackathon.service;

import com.hackathon.dto.history.ExpertHistoryResponse;
import com.hackathon.dto.history.StudentHistoryResponse;
import com.hackathon.security.CustomUserDetails;

public interface AccountService {
    StudentHistoryResponse studentHistory(Integer accountId, CustomUserDetails userDetails);
    ExpertHistoryResponse  expertHistory (Integer accountId, CustomUserDetails userDetails);
}
