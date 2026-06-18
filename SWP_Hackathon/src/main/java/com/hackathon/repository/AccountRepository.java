package com.hackathon.repository;

import com.hackathon.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Component;

@Component
public interface AccountRepository extends JpaRepository<Account, Integer> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    java.util.Optional<Account> findByEmail(String email);

    java.util.Optional<Account> findByVerificationToken(String verificationToken);
    
}
