package com.hackathon.repository;

import com.hackathon.entity.EventCoordinator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

<<<<<<< HEAD
public interface EventCoordinatorRepository extends JpaRepository<EventCoordinator, Integer> {
    Optional<EventCoordinator> findByAccount_AccountId(Integer accountId);
=======
@Component
public interface EventCoordinatorRepository extends JpaRepository<EventCoordinator, Integer> {

    Optional<EventCoordinator> findByAccount_Email(String accountEmail);
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1
}
