package com.hackathon.repository;

import com.hackathon.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;



public interface AccountRepository extends JpaRepository<Account, Integer> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    java.util.Optional<Account> findByEmail(String email);

    java.util.Optional<Account> findByVerificationToken(String verificationToken);

//    boolean exitsByStudent_StudentIdAndLea

}
