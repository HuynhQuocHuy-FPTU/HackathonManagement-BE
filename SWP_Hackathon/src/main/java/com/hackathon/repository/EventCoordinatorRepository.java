package com.hackathon.repository;

import com.hackathon.entity.EventCoordinator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCoordinatorRepository extends JpaRepository<EventCoordinator, Integer> {
}
