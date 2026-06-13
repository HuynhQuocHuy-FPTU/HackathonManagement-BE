package com.hackathon.repository;

import com.hackathon.entity.Account;
import com.hackathon.entity.EventCoordinator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventCoordinatorRepository extends JpaRepository<EventCoordinator, Integer> {
    Optional<EventCoordinator> findByAccount_AccountId(Integer accountId);
    // Hoặc nếu bạn đặt thuộc tính liên kết là 'account' thì dùng:
    Optional<EventCoordinator> findByAccount(Account account);
}
