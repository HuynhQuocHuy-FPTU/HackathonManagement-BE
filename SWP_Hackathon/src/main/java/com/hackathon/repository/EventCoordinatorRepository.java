package com.hackathon.repository;

import com.hackathon.entity.EventCoordinator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
@Component
public interface EventCoordinatorRepository extends JpaRepository<EventCoordinator, Integer> {
    @Query("SELECT ec FROM EventCoordinator ec JOIN FETCH ec.account")
    List<EventCoordinator> findAllWithAccount();

    Optional<EventCoordinator> findByAccount_AccountId(Integer accountId);
    Optional<EventCoordinator> findByAccount_Email(String accountEmail);
    Optional<EventCoordinator> findFirstByOrderByCoordinatorIdAsc();
}
