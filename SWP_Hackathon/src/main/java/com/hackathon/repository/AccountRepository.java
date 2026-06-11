package com.hackathon.repository;

import com.hackathon.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.config.Task;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    Account findByEmail(String email);
}
