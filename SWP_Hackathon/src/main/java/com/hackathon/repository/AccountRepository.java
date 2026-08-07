package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface AccountRepository extends JpaRepository<Account, Integer> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Account> findByEmail(String email);

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

    Optional<Account> findAccountByGithubId(Long githubId);
    @Query("""
            SELECT DISTINCT  s.account
                        FROM CategoryRound cr
                        JOIN cr.teamParticipants tp
                        JOIN tp.registration r
                        JOIN r.team t 
                        JOIN t.teamMembers tm
                        JOIN tm.student s
                        WHERE cr.round.roundId = :roundId 
                        AND s.account IS NOT NULL
            """)
    List<Account> findParticipantsByRoundId(@Param("roundId") Integer roundId);
    List<Account> findByRoleIn(List<AccountRole> roles);

    long countByRole(AccountRole role);
    long countByStatus(AccountStatus status);

    /**
     * TỐI ƯU HÓA: Sử dụng LEFT JOIN FETCH để kéo toàn bộ dữ liệu Profile
     * (Student, Expert, EventCoordinator) trong 1 câu truy vấn duy nhất.
     * Giải quyết triệt để lỗi N+1 Query.
     */
    @Query("SELECT a FROM Account a " +
            "LEFT JOIN FETCH a.student " +
            "LEFT JOIN FETCH a.expert " +
            "LEFT JOIN FETCH a.eventCoordinator " +
            "WHERE a.accountId = :accountId")
    Optional<Account> findByIdWithProfile(@Param("accountId") Integer accountId);
}
