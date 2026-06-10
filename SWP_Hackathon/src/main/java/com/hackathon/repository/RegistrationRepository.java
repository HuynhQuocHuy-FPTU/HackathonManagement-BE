package com.hackathon.repository;

import com.hackathon.entity.Registration;
import com.hackathon.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Integer> {
    Optional<Registration> findByTeam(Team team);

}
