package com.hackathon.repository;

import com.hackathon.entity.HackathonEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HackathonEventRepository extends JpaRepository<HackathonEvent, Integer> {
}
