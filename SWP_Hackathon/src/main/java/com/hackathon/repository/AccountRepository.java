package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.enums.AccountRole;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface AccountRepository extends JpaRepository<Account, Integer> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    java.util.Optional<Account> findByEmail(String email);

    java.util.Optional<Account> findByVerificationToken(String verificationToken);

    @Query("""
                SELECT CASE
                    WHEN a.role = 'STUDENT' THEN s.studentName
                    WHEN a.role = 'EXPERT' THEN e.expertName
                    WHEN a.role = 'EVENTCOORDINATOR' THEN c.coordinatorName
                END
                FROM Account a
                LEFT JOIN Student s ON s.account = a
                LEFT JOIN Expert e ON e.account = a
                LEFT JOIN EventCoordinator c ON c.account = a
                WHERE a.email = :email
            """)
    java.util.Optional<String> findFullNameByEmail(@Param("email") String email);

    List<Account> findAccountByRole(AccountRole role);
}
