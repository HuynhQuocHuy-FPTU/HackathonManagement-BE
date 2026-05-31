package com.hackathon.repository;

import com.hackathon.entity.Expert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpertRepository extends JpaRepository<Expert, Integer> {
}
