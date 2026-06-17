package com.hackathon.repository;

import com.hackathon.entity.EventCoordinator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface EventCoordinatorRepository extends JpaRepository<EventCoordinator, Integer> {

    Optional<EventCoordinator> findByAccount_Email(String accountEmail);
}
