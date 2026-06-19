package com.hackathon.repository;

import com.hackathon.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationEventRepository extends JpaRepository<Registration, Integer> {
}
